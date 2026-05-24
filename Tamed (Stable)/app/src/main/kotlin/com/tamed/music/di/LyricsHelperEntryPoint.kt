/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.di

import com.tamed.music.lyrics.LyricsHelper
import com.tamed.music.lyrics.LyricsPreloadManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LyricsHelperEntryPoint {
    fun lyricsHelper(): LyricsHelper
    fun lyricsPreloadManager(): LyricsPreloadManager
}