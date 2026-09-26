/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.utils

data class AuthScopedCacheValue(
    val url: String,
    val expiresAtMs: Long,
    val authFingerprint: String,
) {
    fun isValidFor(
        authFingerprint: String,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        return this.authFingerprint == authFingerprint && expiresAtMs > nowMs
    }
}
