/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.extensions

fun <T> tryOrNull(block: () -> T): T? =
    try {
        block()
    } catch (e: Exception) {
        null
    }
