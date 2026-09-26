/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.db.entities

sealed class LocalItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnailUrl: String?
}
