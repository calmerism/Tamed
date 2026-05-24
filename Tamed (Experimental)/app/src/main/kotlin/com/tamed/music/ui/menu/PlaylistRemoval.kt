package com.tamed.music.ui.menu

import com.tamed.music.db.entities.PlaylistSongMap
import com.tamed.music.innertube.YouTube

suspend fun removeSongFromRemotePlaylist(
    playlistBrowseId: String,
    playlistSongMap: PlaylistSongMap,
) {
    val setVideoIds =
        playlistSongMap.setVideoId?.let(::listOf)
            ?: YouTube.playlistEntrySetVideoIds(playlistBrowseId, playlistSongMap.songId)
                .getOrDefault(emptyList())

    setVideoIds
        .distinct()
        .forEach { setVideoId ->
            YouTube.removeFromPlaylist(playlistBrowseId, playlistSongMap.songId, setVideoId)
        }
}
