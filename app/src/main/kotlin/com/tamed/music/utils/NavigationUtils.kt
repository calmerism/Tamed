package com.tamed.music.utils

import android.content.Context
import android.net.Uri
import androidx.navigation.NavController

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tamed.music.ui.component.BottomSheetState

/** Flag indicating navigation was triggered from the player sheet to suppress horizontal slide */
var isNavigatingFromPlayer by mutableStateOf(false)

/**
 * Universal artist navigation extension that automatically:
 * 1. Preloads artist wordmark logos and Coil thumbnail caches via [ArtistMemoryCache]
 * 2. URL-encodes optional artist name and thumbnail query parameters
 * 3. Navigates cleanly to the artist screen with instant zero-delay data
 */
fun NavController.navigateToArtist(
    context: Context? = null,
    artistId: String?,
    name: String? = null,
    thumbnailUrl: String? = null,
) {
    if (artistId.isNullOrBlank()) return
    ArtistMemoryCache.preload(context, artistId, name, thumbnailUrl)
    val encodedName = name?.let { Uri.encode(it) }
    val encodedThumb = thumbnailUrl?.let { Uri.encode(it) }
    val queryParams = buildList {
        if (!encodedName.isNullOrBlank()) add("name=$encodedName")
        if (!encodedThumb.isNullOrBlank()) add("thumbnailUrl=$encodedThumb")
    }.joinToString("&")

    val destination = if (queryParams.isNotEmpty()) {
        "artist/$artistId?$queryParams"
    } else {
        "artist/$artistId"
    }
    navigate(destination)
}

/**
 * Seamless player-to-artist navigation that triggers an in-place reveal under the
 * collapsing player sheet instead of a clashing horizontal slide.
 */
fun NavController.navigateToArtistFromPlayer(
    context: Context? = null,
    artistId: String?,
    name: String? = null,
    thumbnailUrl: String? = null,
    sheetState: BottomSheetState? = null,
) {
    if (artistId.isNullOrBlank()) return
    isNavigatingFromPlayer = true
    navigateToArtist(context, artistId, name, thumbnailUrl)
    sheetState?.collapseSoft()
}

/**
 * Seamless player-to-album navigation that triggers an in-place reveal under the
 * collapsing player sheet instead of a clashing horizontal slide.
 */
fun NavController.navigateToAlbumFromPlayer(
    albumId: String?,
    sheetState: BottomSheetState? = null,
) {
    if (albumId.isNullOrBlank()) return
    isNavigatingFromPlayer = true
    navigate("album/$albumId")
    sheetState?.collapseSoft()
}
