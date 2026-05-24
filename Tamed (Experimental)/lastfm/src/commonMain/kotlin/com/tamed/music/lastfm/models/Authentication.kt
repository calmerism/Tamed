/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class Authentication(
    val session: Session,
) {
    @Serializable
    data class Session(
        val name: String,
        val key: String,
        val subscriber: Int,
    )
}

@Serializable
data class TokenResponse(
    val token: String,
)

@Serializable
data class LastFmError(
    val error: Int,
    val message: String,
)
