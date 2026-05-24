/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val id: String,
    val lyrics: String,
) {
    companion object {
        const val LYRICS_NOT_FOUND = "LYRICS_NOT_FOUND"
    }
}
