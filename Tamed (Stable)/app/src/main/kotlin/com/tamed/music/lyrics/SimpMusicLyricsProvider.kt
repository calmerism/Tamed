/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.lyrics

import android.content.Context
import com.tamed.music.constants.EnableSimpMusicLyricsKey
import com.tamed.music.simpmusic.SimpMusicLyrics
import com.tamed.music.utils.dataStore
import com.tamed.music.utils.get

object SimpMusicLyricsProvider : LyricsProvider {
    override val name: String = "SimpMusic"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableSimpMusicLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = SimpMusicLyrics.getLyrics(videoId = id, duration = duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        SimpMusicLyrics.getAllLyrics(videoId = id, duration = duration, callback = callback)
    }
}

