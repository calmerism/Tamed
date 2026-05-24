/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.lyrics

import android.content.Context
import com.tamed.music.kugou.KuGou
import com.tamed.music.constants.EnableKugouKey
import com.tamed.music.utils.dataStore
import com.tamed.music.utils.get

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = KuGou.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        KuGou.getAllPossibleLyricsOptions(title, artist, duration, callback)
    }
}
