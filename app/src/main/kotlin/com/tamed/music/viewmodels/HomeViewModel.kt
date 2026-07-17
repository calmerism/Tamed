package com.tamed.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.innertube.models.PlaylistItem
import com.tamed.music.innertube.models.WatchEndpoint
import com.tamed.music.innertube.models.YTItem
import com.tamed.music.innertube.models.filterExplicit
import com.tamed.music.innertube.models.filterVideo
import com.tamed.music.innertube.pages.ExplorePage
import com.tamed.music.innertube.pages.HomePage
import com.tamed.music.innertube.utils.completed
import com.tamed.music.innertube.utils.parseCookieString
import com.tamed.music.constants.HideExplicitKey
import com.tamed.music.constants.HideVideoKey
import com.tamed.music.constants.InnerTubeCookieKey
import com.tamed.music.constants.QuickPicks
import com.tamed.music.constants.QuickPicksKey
import com.tamed.music.constants.SpeedDialSongIdsKey
import com.tamed.music.constants.YtmSyncKey
import com.tamed.music.db.MusicDatabase
import com.tamed.music.db.entities.*
import com.tamed.music.extensions.toEnum
import com.tamed.music.models.SimilarRecommendation
import com.tamed.music.models.HomeLocalShelf
import com.tamed.music.utils.dataStore
import com.tamed.music.utils.get
import com.tamed.music.utils.getAsync
import com.tamed.music.utils.SyncUtils
import com.tamed.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import com.tamed.music.models.MediaMetadata
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.models.toSong
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    private val isInitialLoadComplete = MutableStateFlow(false)

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val speedDialSongs = MutableStateFlow<List<Song>>(emptyList())
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val recentlyPlayed = MutableStateFlow<List<LocalItem>>(emptyList())
    val contextualShelves = MutableStateFlow<List<HomeLocalShelf>>(emptyList())
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val recommendedAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val recentPlaylistsDb = MutableStateFlow<List<Playlist>?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    private val _isRandomizing = MutableStateFlow(false)
    val isRandomizing = _isRandomizing.asStateFlow()

    // Account display info
    val accountName = MutableStateFlow("")
    val accountImageUrl = MutableStateFlow<String?>(null)
    val isAccountLoading = MutableStateFlow(true)
    val isAccountLoggedIn = MutableStateFlow(false)
    
    // Track last processed cookie to avoid unnecessary updates
    private var lastProcessedCookie: String? = null
    
    // Track if we're currently processing account data
    private var isProcessingAccountData = false
    private var wasLoggedIn = false
    private var lastArtistAlbumRefreshMs = 0L

    private fun filterHomeChips(chips: List<HomePage.Chip>?): List<HomePage.Chip>? {
        return chips?.filterNot { it.title.contains("podcasts", ignoreCase = true) }
    }

    private suspend fun getQuickPicks(){
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> {
                // Instantly load from local database first to avoid startup blocking
                val localPicks = database.quickPicks().first().shuffled().take(20)
                if (localPicks.isNotEmpty()) {
                    quickPicks.value = localPicks
                }
                
                // Then fetch remote updates in the background
                viewModelScope.launch(Dispatchers.IO) {
                    val recentSong = database.events().first().firstOrNull()?.song
                    if (recentSong != null) {
                        val endpoint = WatchEndpoint(videoId = recentSong.id)
                        YouTube.next(endpoint).getOrNull()?.let { nextResult ->
                            YouTube.related(nextResult.relatedEndpoint ?: return@let).getOrNull()?.let { relatedPage ->
                                 val remoteSongs = relatedPage.songs.take(20).map { it.toMediaMetadata().toSong() }
                                 if (remoteSongs.isNotEmpty()) {
                                     quickPicks.value = remoteSongs
                                 }
                            }
                        }
                    }
                }
            }
            QuickPicks.LAST_LISTEN -> songLoad()
            QuickPicks.DONT_SHOW -> quickPicks.value = null
        }
    }

    private suspend fun loadSpeedDialSongs() {
        val speedDialIds = context.dataStore.getAsync(SpeedDialSongIdsKey, "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(17)
        
        val pinnedSongs = if (speedDialIds.isNotEmpty()) {
            val songsById = database.getSongsByIds(speedDialIds).associateBy { it.id }
            speedDialIds.mapNotNull { songsById[it] }.distinctBy { it.id }
        } else {
            emptyList()
        }

        // Always fill up to 17 slots. 
        // Prioritize: 1. Pinned 2. Discovery Picks (New tracks) 3. Most Played
        if (pinnedSongs.size < 17) {
            val discoveryPicks = quickPicks.value.orEmpty()
            val mostPlayed = database.mostPlayedSongs(0, limit = 34).first()
            
            // Mix discovery and most played, but prioritize discovery for the backfill
            val backfill = (discoveryPicks + mostPlayed)
                .distinctBy { it.id }
                .filterNot { bc -> pinnedSongs.any { ps -> ps.id == bc.id } }
            
            speedDialSongs.value = (pinnedSongs + backfill).take(17)
        } else {
            speedDialSongs.value = pinnedSongs.take(17)
        }
    }
    private suspend fun refreshKeepListening(fromTimeStamp: Long) {
        val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
            .first().shuffled().take(10)
        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2)
            .first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp)
            .first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }
            .shuffled().take(5)
        keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()
    }

    private suspend fun refreshLocalHomeData() {
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 14
        getQuickPicks()
        loadSpeedDialSongs()
        forgottenFavorites.value = database.forgottenFavorites().first().shuffled().take(20)
        loadRecentlyPlayed()
        refreshKeepListening(fromTimeStamp)
        loadContextualShelves()

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Album }
    }

    
    private suspend fun refreshArtistAlbumRecommendations(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastArtistAlbumRefreshMs < 5 * 60_000L) return
        lastArtistAlbumRefreshMs = now

        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideo = context.dataStore.get(HideVideoKey, false)
        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2

        // Vivi's recommendation logic port
        val artistRecommendations = database.mostPlayedArtists(fromTimeStamp, limit = 15).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(4)
            .mapNotNull {
                val items = mutableListOf<YTItem>()
                YouTube.artist(it.id).getOrNull()?.let { page ->
                    page.sections.takeLast(3).forEach { section ->
                        items += section.items
                    }
                }
                SimilarRecommendation(
                    title = it,
                    items = items.distinctBy { item -> item.id }
                        .filterExplicit(hideExplicit).filterVideo(hideVideo)
                        .shuffled().take(12)
                )
            }.filter { it.items.isNotEmpty() }

        val songRecommendations = database.mostPlayedSongs(fromTimeStamp, limit = 15).first()
            .filter { it.album != null }
            .shuffled().take(3)
            .mapNotNull { song ->
                val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint ?: return@mapNotNull null
                val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                SimilarRecommendation(
                    title = song,
                    items = (page.songs.shuffled().take(10) +
                            page.albums.shuffled().take(5) +
                            page.artists.shuffled().take(3) +
                            page.playlists.shuffled().take(3))
                        .distinctBy { it.id }.filterExplicit(hideExplicit).filterVideo(hideVideo)
                        .shuffled()
                )
            }.filter { it.items.isNotEmpty() }

        val albumRecommendations = database.mostPlayedAlbums(fromTimeStamp, limit = 10).first()
            .filter { it.album.thumbnailUrl != null }
            .shuffled().take(2)
            .mapNotNull { album ->
                val items = mutableListOf<YTItem>()
                YouTube.album(album.id).getOrNull()?.let { page ->
                    items += page.otherVersions
                }
                album.artists.firstOrNull()?.id?.let { artistId ->
                    YouTube.artist(artistId).getOrNull()?.sections?.lastOrNull()?.items?.let { items += it }
                }
                SimilarRecommendation(
                    title = album,
                    items = items.distinctBy { it.id }
                        .filterExplicit(hideExplicit).filterVideo(hideVideo)
                        .shuffled().take(10)
                )
            }.filter { it.items.isNotEmpty() }

        similarRecommendations.value = (artistRecommendations + songRecommendations + albumRecommendations).shuffled()

        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
            homePage.value?.sections?.flatMap { it.items }.orEmpty()
    }
private suspend fun refreshRemoteHomeData() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideo = context.dataStore.get(HideVideoKey, false)

        supervisorScope {
            launch {
                YouTube.home().onSuccess { page ->
                    homePage.value = page.copy(
                        chips = filterHomeChips(page.chips),
                        sections = page.sections.map { section ->
                            section.copy(items = section.items.filterExplicit(hideExplicit).filterVideo(hideVideo))
                        }
                    )
                }.onFailure { reportException(it) }
            }

            launch {
                YouTube.explore().onSuccess { page ->
                    val artists: MutableMap<Int, String> = mutableMapOf()
                    val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                    database.allArtistsByPlayTime().first().let { list ->
                        var favIndex = 0
                        for ((artistsIndex, artist) in list.withIndex()) {
                            artists[artistsIndex] = artist.id
                            if (artist.artist.bookmarkedAt != null) {
                                favouriteArtists[favIndex] = artist.id
                                favIndex++
                            }
                        }
                    }
                    explorePage.value = page.copy(
                        newReleaseAlbums = page.newReleaseAlbums
                            .sortedBy { album ->
                                val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                val firstArtistKey = artistIds.firstNotNullOfOrNull { artistId ->
                                    if (artistId in favouriteArtists.values) {
                                        favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                    } else {
                                        artists.entries.firstOrNull { it.value == artistId }?.key
                                    }
                                } ?: Int.MAX_VALUE
                                firstArtistKey
                            }.filterExplicit(hideExplicit)
                    )
                }.onFailure { reportException(it) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            
            refreshArtistAlbumRecommendations(force = true)
        }

        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
            homePage.value?.sections?.flatMap { it.items }.orEmpty()
    }


    private suspend fun load(remoteSynchronous: Boolean = false) {
        if (isLoading.value) return
        isLoading.value = true

        try {
            refreshLocalHomeData()
            isInitialLoadComplete.value = true
            if (remoteSynchronous) {
                refreshRemoteHomeData()
            } else {
                isLoading.value = false
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        refreshRemoteHomeData()
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
        } catch (e: Exception) {
            reportException(e)
        } finally {
            isLoading.value = false
        }
    }

    private suspend fun songLoad() {
        val song = database.events().first().firstOrNull()?.song
        if (song != null) {
            if (database.hasRelatedSongs(song.id)) {
                val relatedSongs = database.getRelatedSongs(song.id).first().shuffled().take(20)
                quickPicks.value = relatedSongs
            }
        }
    }

    private suspend fun loadRecentlyPlayed() {
        val recentEvents = database.events().first()
        val recentAlbums = recentEvents
            .map { it.song }
            .distinctBy { it.id }
            .take(24)
            .mapNotNull { song ->
                val album = song.album ?: return@mapNotNull null
                Album(
                    album = album,
                    artists = song.artists,
                )
            }
            .distinctBy { it.id }
            .take(12)

        recentlyPlayed.value = recentAlbums
    }

    private suspend fun loadContextualShelves() {
        val recentEvents = database.events().first()
        val recentSongs = recentEvents.map { it.song }.distinctBy { it.id }
        val topArtists = database.mostPlayedArtists(
            fromTimeStamp = System.currentTimeMillis() - 86400000L * 21,
            limit = 8,
        ).first()
        val topSongs = database.mostPlayedSongs(
            fromTimeStamp = System.currentTimeMillis() - 86400000L * 21,
            limit = 12,
        ).first()

        val shelves = buildList {
            val anchorSong = recentSongs.firstOrNull() ?: topSongs.firstOrNull()
            if (anchorSong != null) {
                val relatedSongs = if (database.hasRelatedSongs(anchorSong.id)) {
                    database.getRelatedSongs(anchorSong.id).first()
                } else {
                    val artistIds = anchorSong.artists.map { it.id }.toSet()
                    database.allSongs().first()
                        .filter { candidate ->
                            candidate.id != anchorSong.id &&
                                candidate.artists.any { it.id in artistIds }
                        }
                }

                if (relatedSongs.isNotEmpty()) {
                    val relatedAlbums = relatedSongs
                        .mapNotNull { candidate ->
                            candidate.album?.let { album -> Album(album = album, artists = candidate.artists) }
                        }
                        .distinctBy { it.id }
                        .take(10)

                    if (relatedAlbums.isNotEmpty()) {
                        add(
                            HomeLocalShelf(
                                title = "Because you listened to ${anchorSong.album?.title ?: anchorSong.song.title}",
                                subtitle = anchorSong.artists.joinToString { it.name },
                                anchorTitle = anchorSong.album?.title ?: anchorSong.song.title,
                                items = relatedAlbums,
                            )
                        )
                    }
                }
            }

            val dayPart = currentDayPart()
            val sameDayPartSongs = recentEvents
                .filter { eventWithSong ->
                    val hour = eventWithSong.event.timestamp.hour
                    when (dayPart) {
                        "morning" -> hour in 5..11
                        "afternoon" -> hour in 12..16
                        "evening" -> hour in 17..21
                        else -> hour !in 5..21
                    }
                }
                .map { it.song }
                .distinctBy { it.id }
                .take(10)

            if (sameDayPartSongs.isNotEmpty()) {
                val dayPartAlbums = sameDayPartSongs
                    .mapNotNull { song ->
                        song.album?.let { album -> Album(album = album, artists = song.artists) }
                    }
                    .distinctBy { it.id }
                    .take(10)

                if (dayPartAlbums.isNotEmpty()) {
                    add(
                        HomeLocalShelf(
                            title = "Made for your ${dayPart}",
                            subtitle = "Albums you reach for around this time",
                            anchorTitle = dayPart,
                            items = dayPartAlbums,
                        )
                    )
                }
            }
        }

        contextualShelves.value = shelves
    }

    private fun currentDayPart(now: LocalDateTime = LocalDateTime.now()): String =
        when (now.hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..21 -> "evening"
            else -> "night"
        }

    private fun clearAccountData() {
        accountName.value = ""
        accountImageUrl.value = null
        accountPlaylists.value = null
    }

    private fun prepareYouTubeAccount(cookie: String): Boolean {
        return try {
            YouTube.cookie = cookie
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to set YouTube cookie")
            false
        }
    }

    private suspend fun refreshAccountIdentity() {
        accountName.value = ""
        accountImageUrl.value = null

        try {
            YouTube.accountInfo().onSuccess { info ->
                accountName.value = info.name
                accountImageUrl.value = info.thumbnailUrl
            }.onFailure { error ->
                Timber.w(error, "Failed to fetch account info")
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception fetching account info")
        }
    }

    private suspend fun refreshAccountPlaylistsInternal() {
        try {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                val lists = it.items.filterIsInstance<PlaylistItem>().filterNot { playlist ->
                    playlist.id == "SE"
                }
                accountPlaylists.value = lists
            }.onFailure { error ->
                Timber.w(error, "Failed to fetch account playlists")
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception fetching account playlists")
        }
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideo = context.dataStore.get(HideVideoKey, false)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            val nextSections = YouTube.home(continuation).getOrNull() ?: run {
                _isLoadingMore.value = false
                return@launch
            }

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = (homePage.value?.sections.orEmpty() + nextSections.sections).map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideo(hideVideo))
                }
            )
            _isLoadingMore.value = false
        }
    }

    fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
            homePage.value = previousHomePage.value
            previousHomePage.value = null
            selectedChip.value = null
            return
        }

        if (selectedChip.value == null) {
            previousHomePage.value = homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideo = context.dataStore.get(HideVideoKey, false)
            val nextSections = YouTube.home(params = chip?.endpoint?.params).getOrNull() ?: return@launch

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = nextSections.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideo(hideVideo))
                }
            )
            selectedChip.value = chip
        }
    }

    fun randomizeHome() {
        if (_isRandomizing.value) return
        
        viewModelScope.launch {
            _isRandomizing.value = true
            kotlinx.coroutines.delay(950)
            
            // Shuffle existing collections
            quickPicks.value?.let { quickPicks.value = it.shuffled() }
            keepListening.value?.let { keepListening.value = it.shuffled() }
            forgottenFavorites.value?.let { forgottenFavorites.value = it.shuffled() }
            
            val currentSimilar = similarRecommendations.value
            if (currentSimilar != null) {
                similarRecommendations.value = currentSimilar.shuffled().map { 
                    it.copy(items = it.items.shuffled()) 
                }
            }
            
            val currentHomePage = homePage.value
            if (currentHomePage != null) {
                homePage.value = currentHomePage.copy(
                    sections = currentHomePage.sections.shuffled().map {
                        it.copy(items = it.items.shuffled())
                    }
                )
            }
            
            val currentExplore = explorePage.value
            if (currentExplore != null) {
                explorePage.value = currentExplore.copy(
                    newReleaseAlbums = currentExplore.newReleaseAlbums.shuffled()
                )
            }
            
            _isRandomizing.value = false
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load(remoteSynchronous = true)
            isRefreshing.value = false
        }
    }

    fun refreshAccountData() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isProcessingAccountData) return@launch
            
            isProcessingAccountData = true
            isAccountLoading.value = true
            try {
                val cookie = context.dataStore.get(InnerTubeCookieKey, "")
                val loggedIn = cookie.isNotEmpty() && "SAPISID" in parseCookieString(cookie)
                isAccountLoggedIn.value = loggedIn

                if (loggedIn && prepareYouTubeAccount(cookie)) {
                    refreshAccountIdentity()
                    launch {
                        refreshAccountPlaylistsInternal()
                    }
                } else {
                    clearAccountData()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing account data")
                clearAccountData()
            } finally {
                isAccountLoading.value = false
                isProcessingAccountData = false
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            load(remoteSynchronous = false)
        }

        viewModelScope.launch(Dispatchers.IO) {
            database.events()
                .debounce(500)
                .collect {
                    refreshLocalHomeData()
                    refreshArtistAlbumRecommendations()
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            quickPicksEnum.collect {
                refreshLocalHomeData()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[SpeedDialSongIdsKey].orEmpty() }
                .distinctUntilChanged()
                .collect {
                    refreshLocalHomeData()
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(3000)
            
            syncUtils.cleanupDuplicatePlaylists()
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    if (isProcessingAccountData) return@collect
                    
                    lastProcessedCookie = cookie
                    isProcessingAccountData = true
                    isAccountLoading.value = true
                    
                    try {
                        val isLoggedIn = cookie?.let { "SAPISID" in parseCookieString(it) } ?: false
                        val loginTransition = isLoggedIn && !wasLoggedIn
                        wasLoggedIn = isLoggedIn
                        isAccountLoggedIn.value = isLoggedIn
                        
                        if (isLoggedIn && cookie != null && cookie.isNotEmpty()) {
                            if (!prepareYouTubeAccount(cookie)) {
                                clearAccountData()
                                return@collect
                            }

                            val shouldReloadHome =
                                homePage.value == null || explorePage.value == null || !isInitialLoadComplete.value

                            if (loginTransition) {
                                launch {
                                    try {
                                        if (context.dataStore.get(YtmSyncKey, true)) {
                                            syncUtils.performFullSync()
                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e, "Error during login-triggered sync")
                                        reportException(e)
                                    }
                                }
                            }
                            
                            kotlinx.coroutines.delay(100)

                            refreshAccountIdentity()

                            launch {
                                refreshAccountPlaylistsInternal()
                            }

                            if (shouldReloadHome) {
                                load(remoteSynchronous = false)
                            }
                        } else {
                            clearAccountData()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error processing cookie change")
                        clearAccountData()
                        isAccountLoggedIn.value = false
                    } finally {
                        isAccountLoading.value = false
                        isProcessingAccountData = false
                    }
                }
        }
    }
}
