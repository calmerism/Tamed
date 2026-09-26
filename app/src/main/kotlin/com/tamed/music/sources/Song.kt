package com.tamed.music.sources

data class Song(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val durationText: String? = null,
    val artistId: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val isVideo: Boolean = false,
    val isVideoOrigin: Boolean = isVideo,
    val setVideoId: String? = null,
    val fromAutoplay: Boolean = false,
    val radioName: String? = null,
    val localUri: String? = null,
    val downloadFormat: String? = null,
    val localPath: String? = null,
    val localDateAddedSeconds: Long? = null,
    val localDateModifiedSeconds: Long? = null,
    val sourceQuality: String? = null,
    val isExplicit: Boolean? = null,
)

fun Song.artworkAt(px: Int): String? = thumbnailUrl?.replace(Regex("""w\d+-h\d+"""), "w$px-h$px")

fun Song.isSameTrackAs(other: Song?): Boolean {
    other ?: return false
    return title == other.title && artist == other.artist
}

fun Song.durationMillis(): Long = durationText.durationMillis()

fun String?.durationMillis(): Long {
    val parts = this?.trim()?.takeIf { it.isNotEmpty() }?.split(":") ?: return 0L
    val numbers = parts.map { it.trim().toLongOrNull() ?: return 0L }
    val seconds = when (numbers.size) {
        2 -> numbers[0] * 60 + numbers[1]
        3 -> numbers[0] * 3_600 + numbers[1] * 60 + numbers[2]
        else -> return 0L
    }
    return (seconds * 1_000).coerceAtLeast(0L)
}
