/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tamed.music.innertube.YouTube
import com.tamed.music.constants.statToPeriod
import com.tamed.music.db.MusicDatabase
import com.tamed.music.ui.screens.OptionStats
import com.tamed.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

private fun activePeriodEnd(selection: OptionStats, index: Int): Long =
    if (selection == OptionStats.CONTINUOUS || index == 0) {
        Long.MAX_VALUE
    } else {
        statToPeriod(selection, index - 1)
    }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel
@Inject
constructor(
    val database: MusicDatabase,
) : ViewModel() {
    val selectedOption = MutableStateFlow(OptionStats.CONTINUOUS)
    val indexChips = MutableStateFlow(0)

    val mostPlayedSongsStats =
        combine(
            selectedOption,
            indexChips,
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database
                    .mostPlayedSongsStats(
                        fromTimeStamp = statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp = activePeriodEnd(selection, t),
                    )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedSongs =
        combine(
            selectedOption,
            indexChips,
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database
                    .mostPlayedSongs(
                        fromTimeStamp = statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp = activePeriodEnd(selection, t),
                    )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedArtists =
        combine(
            selectedOption,
            indexChips,
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database
                    .mostPlayedArtists(
                        statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp = activePeriodEnd(selection, t),
                    ).map { artists ->
                        artists.filter { it.artist.isYouTubeArtist }
                    }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedAlbums =
        combine(
            selectedOption,
            indexChips,
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database.mostPlayedAlbums(
                    statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp = activePeriodEnd(selection, t),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val firstEvent =
        database
            .firstEvent()
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            mostPlayedArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null || Duration.between(
                            it.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
        viewModelScope.launch {
            mostPlayedAlbums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
    }
}
