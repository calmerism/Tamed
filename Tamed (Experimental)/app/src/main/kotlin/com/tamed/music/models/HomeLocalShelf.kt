/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.models

import com.tamed.music.db.entities.LocalItem

data class HomeLocalShelf(
    val title: String,
    val subtitle: String,
    val anchorTitle: String,
    val items: List<LocalItem>,
)
