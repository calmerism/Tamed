/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.tamed.music.constants.InnerTubeCookieKey
import com.tamed.music.constants.YtmSyncKey
import com.tamed.music.utils.dataStore
import com.tamed.music.utils.get
import com.tamed.music.innertube.utils.parseCookieString

fun Context.isSyncEnabled(): Boolean {
    return dataStore.get(YtmSyncKey, true) && isUserLoggedIn()
}

fun Context.isUserLoggedIn(): Boolean {
    val cookie = dataStore[InnerTubeCookieKey] ?: ""
    return "SAPISID" in parseCookieString(cookie) && isInternetConnected()
}

fun Context.isInternetConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
