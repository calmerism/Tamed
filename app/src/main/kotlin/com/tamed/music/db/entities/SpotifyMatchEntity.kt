package com.tamed.music.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Caches the mapping between a Spotify track ID and the best-matching YouTube video ID.
 */
@Entity(tableName = "spotify_match", indices = [Index(value = ["youtubeId"])])
data class SpotifyMatchEntity(
    @PrimaryKey val spotifyId: String,
    val youtubeId: String,
    val title: String,
    val artist: String,
    val matchScore: Double,
    val cachedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val isManualOverride: Boolean = false,
)
