/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.utils

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.tamed.music.constants.AccountChannelHandleKey
import com.tamed.music.constants.AccountEmailKey
import com.tamed.music.constants.AccountNameKey
import com.tamed.music.constants.DataSyncIdKey
import com.tamed.music.constants.InnerTubeCookieKey
import com.tamed.music.constants.PoTokenGvsKey
import com.tamed.music.constants.PoTokenKey
import com.tamed.music.constants.PoTokenPlayerKey
import com.tamed.music.constants.PoTokenSourceUrlKey
import com.tamed.music.constants.VisitorDataKey
import com.tamed.music.constants.WebClientPoTokenEnabledKey
import com.tamed.music.innertube.PlaybackAuthState

fun Preferences.toPlaybackAuthState(): PlaybackAuthState =
    PlaybackAuthState(
        cookie = this[InnerTubeCookieKey],
        visitorData = this[VisitorDataKey],
        dataSyncId = this[DataSyncIdKey],
        poToken = this[PoTokenKey],
        poTokenGvs = this[PoTokenGvsKey],
        poTokenPlayer = this[PoTokenPlayerKey],
        webClientPoTokenEnabled = this[WebClientPoTokenEnabledKey] ?: false,
    ).normalized()

fun MutablePreferences.clearPlaybackAuthSession(clearAccountIdentity: Boolean = true) {
    remove(InnerTubeCookieKey)
    remove(VisitorDataKey)
    remove(DataSyncIdKey)
    remove(PoTokenKey)
    remove(PoTokenGvsKey)
    remove(PoTokenPlayerKey)
    remove(PoTokenSourceUrlKey)
    if (clearAccountIdentity) {
        remove(AccountNameKey)
        remove(AccountEmailKey)
        remove(AccountChannelHandleKey)
    }
}

fun MutablePreferences.clearPlaybackLoginContext() {
    remove(DataSyncIdKey)
}

fun MutablePreferences.putLegacyPoToken(value: String?) {
    val normalized = value?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    if (normalized == null) {
        remove(PoTokenKey)
    } else {
        this[PoTokenKey] = normalized
    }
    remove(PoTokenGvsKey)
    remove(PoTokenPlayerKey)
}
