/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this

    // i.ytimg.com handling (YouTube video thumbnails)
    if (this.contains("i.ytimg.com")) {
        val targetQuality = if (width != null && width >= 1200) "maxresdefault.jpg" else "hqdefault.jpg"
        return this.replace(
            Regex("(default|mqdefault|hqdefault|sddefault|maxresdefault)\\.jpg"),
            targetQuality
        )
    }

    // googleusercontent.com handling (includes lh3-lh6, yt3, etc.)
    if (this.contains("googleusercontent.com") && this.contains("=w")) {
        val baseUrl = this.split("=w")[0]
        val w = width ?: 0
        val h = height ?: width ?: 0
        return "$baseUrl=w$w-h$h-p-l90-rj"
    }

    // yt3.ggpht.com handling (avatars)
    if (this.contains("yt3.ggpht.com")) {
        val baseUrl = this.split("=")[0].split("-s")[0]
        return "$baseUrl=s${width ?: height}"
    }

    // Fallback for other lh3-style URLs that might not have =w yet
    "https://lh\\d\\.googleusercontent\\.com/.*".toRegex().matchEntire(this)?.let {
        val w = width ?: 0
        val h = height ?: width ?: 0
        return "${this.split("=")[0]}=w$w-h$h-p-l90-rj"
    }

    return this
}
