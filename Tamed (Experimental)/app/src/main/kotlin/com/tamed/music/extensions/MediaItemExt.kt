/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.extensions

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.tamed.music.innertube.models.SongItem
import com.tamed.music.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.tamed.music.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_UGC
import com.tamed.music.db.entities.Song
import com.tamed.music.models.MediaMetadata
import com.tamed.music.models.toMediaMetadata
import com.tamed.music.ui.utils.resize

const val ExtraIsMusicVideo = "com.tamed.music.extra.IS_MUSIC_VIDEO"
private const val NotificationArtworkSizePx = 1200

val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

private fun String?.toNotificationArtworkUri() = this?.resize(NotificationArtworkSizePx, NotificationArtworkSizePx)?.toUri()

fun Song.toMediaItem() =
    toMediaMetadata().toMediaItem(isMusicVideo = false)

fun SongItem.toMediaItem() =
    toMediaMetadata().toMediaItem(isMusicVideo = isMusicVideo())

fun MediaMetadata.toMediaItem(isMusicVideo: Boolean = false) =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(playbackUri())
        .setCustomCacheKey(id)
        .setTag(this)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(thumbnailUrl.toNotificationArtworkUri())
                .setAlbumTitle(album?.title)
                .setIsPlayable(true)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, isMusicVideo) })
                .build(),
        ).build()

private fun SongItem.isMusicVideo(): Boolean {
    val musicVideoType = endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
    return musicVideoType == MUSIC_VIDEO_TYPE_OMV || musicVideoType == MUSIC_VIDEO_TYPE_UGC
}
