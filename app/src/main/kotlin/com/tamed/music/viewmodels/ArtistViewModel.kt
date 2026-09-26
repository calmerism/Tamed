/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.filterExplicit
import com.tamed.music.innertube.pages.ArtistPage
import com.tamed.music.db.MusicDatabase
import com.tamed.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import com.tamed.music.constants.HideExplicitKey
import com.tamed.music.extensions.filterExplicit
import com.tamed.music.extensions.filterExplicitAlbums
import com.tamed.music.utils.dataStore
import com.tamed.music.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    var artistPage by mutableStateOf<ArtistPage?>(com.tamed.music.utils.ArtistMemoryCache.getPage(artistId))
    val initialThumbnail: String? = savedStateHandle.get<String>("thumbnailUrl")
        ?: com.tamed.music.utils.ArtistMemoryCache.getThumbnail(artistId)
        ?: com.tamed.music.ui.theme.BackdropCache.get(artistId)
    val initialName: String? = savedStateHandle.get<String>("name")
        ?: com.tamed.music.utils.ArtistMemoryCache.getName(artistId)
    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val librarySongs = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistSongsByCreateDateAsc(artistId).map { it.filterExplicit(hideExplicit) } // show all
            // database.artistSongsPreview(artistId).map { it.filterExplicit(hideExplicit) } // only preview
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val libraryAlbums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistAlbumsPreview(artistId).map { it.filterExplicitAlbums(hideExplicit) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Load artist page and reload when hide explicit setting changes
        viewModelScope.launch {
            context.dataStore.data
                .map { it[HideExplicitKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    fetchArtistsFromYTM()
                }
        }
    }

    fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            YouTube.artist(artistId)
                .onSuccess { page ->
                    val filteredSections = page.sections
                        .map { section ->
                            section.copy(items = section.items.filterExplicit(hideExplicit))
                        }

                    val updatedPage = page.copy(sections = filteredSections)
                    artistPage = updatedPage
                    com.tamed.music.utils.ArtistMemoryCache.putPage(artistId, updatedPage)
                    viewModelScope.launch {
                        com.tamed.music.ui.screens.artist.ArtistLogoRegistry.getLogo(page.artist.title, artistId)
                    }
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
