/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.db.entities

import androidx.compose.runtime.Immutable

@Immutable
data class SongWithStats(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val songCountListened: Int,
    val timeListened: Long?,
)
