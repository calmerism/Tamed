/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.models.AlbumItem
import com.tamed.music.db.MusicDatabase
import com.tamed.music.utils.AppleMusicAboutAlbum
import com.tamed.music.utils.Wikipedia
import com.tamed.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AlbumUiState {
    data object Loading : AlbumUiState

    data object Content : AlbumUiState

    data object Empty : AlbumUiState

    data class Error(
        val isNotFound: Boolean = false,
    ) : AlbumUiState
}

private sealed interface FetchState {
    data object Pending : FetchState
    data object Success : FetchState
    data class Failed(val isNotFound: Boolean = false) : FetchState
}

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val playlistId = MutableStateFlow("")
    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())
    val albumDescription = MutableStateFlow<String?>(null)

    private val _fetchState = MutableStateFlow<FetchState>(FetchState.Pending)

    val uiState: StateFlow<AlbumUiState> =
        combine(albumWithSongs, _fetchState) { data, fetch ->
            when {
                data != null && data.songs.isNotEmpty() -> AlbumUiState.Content
                fetch is FetchState.Pending -> AlbumUiState.Loading
                fetch is FetchState.Failed && data == null -> AlbumUiState.Error(fetch.isNotFound)
                fetch is FetchState.Success && data != null && data.songs.isEmpty() -> AlbumUiState.Empty
                fetch is FetchState.Failed && data != null && data.songs.isNotEmpty() -> AlbumUiState.Content
                else -> AlbumUiState.Loading
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AlbumUiState.Loading)

    init {
        retry()
    }

    fun retry() {
        viewModelScope.launch {
            _fetchState.value = FetchState.Pending
            val album = database.album(albumId).first()
            YouTube
                .album(albumId)
                .onSuccess {
                    playlistId.value = it.album.playlistId
                    otherVersions.value = it.otherVersions
                    albumDescription.value = it.description
                    database.withTransaction {
                        if (album == null) {
                            insert(it)
                        } else {
                            update(album.album, it, album.artists)
                        }
                    }
                    _fetchState.value = FetchState.Success
                    if (albumDescription.value.isNullOrBlank()) {
                        viewModelScope.launch(Dispatchers.IO) {
                            val artistName = album?.artists?.firstOrNull()?.name
                                ?: database.albumWithSongs(albumId).first()?.artists?.firstOrNull()?.name
                                ?: it.album.artists?.firstOrNull()?.name
                            albumDescription.value =
                                Wikipedia.fetchAlbumInfo(it.album.title, artistName)
                                    ?: AppleMusicAboutAlbum.fetchAlbumDescription(it.album.title, artistName)
                        }
                    }
                }.onFailure {
                    reportException(it)
                    val isNotFound = it.message?.contains("NOT_FOUND") == true
                    if (isNotFound) {
                        database.query {
                            album?.album?.let(::delete)
                        }
                    }
                    _fetchState.value = FetchState.Failed(isNotFound = isNotFound)
                }
        }
    }
}
