/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.spotiflac

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.tamed.music.R

object SpotiFlacHandoff {
    private const val PackageName = "com.zarz.spotiflac"
    private const val DownloadUrl = "https://spotiflac.zarz.moe/"

    @StringRes
    fun openForTrack(
        context: Context,
        title: String,
        artists: List<String>,
    ): Int = messageFor(openForQuery(context, buildTrackQuery(title, artists)))

    @StringRes
    fun openForAlbum(
        context: Context,
        albumTitle: String,
        artists: List<String>,
    ): Int = messageFor(openForQuery(context, buildAlbumQuery(albumTitle, artists)))

    private fun openForQuery(
        context: Context,
        query: String,
    ): LaunchResult {
        copyQueryToClipboard(context, query)

        val packageManager = context.packageManager
        val appIntent =
            packageManager.getLaunchIntentForPackage(PackageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (appIntent != null) {
            return runCatching {
                context.startActivity(appIntent)
                LaunchResult.APP_OPENED
            }.getOrElse { LaunchResult.FAILED }
        }

        val websiteIntent =
            Intent(Intent.ACTION_VIEW, DownloadUrl.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        return if (websiteIntent.resolveActivity(packageManager) != null) {
            runCatching {
                context.startActivity(websiteIntent)
                LaunchResult.DOWNLOAD_PAGE_OPENED
            }.getOrElse { LaunchResult.FAILED }
        } else {
            LaunchResult.FAILED
        }
    }

    @StringRes
    private fun messageFor(result: LaunchResult): Int =
        when (result) {
            LaunchResult.APP_OPENED -> R.string.spotiflac_opened
            LaunchResult.DOWNLOAD_PAGE_OPENED -> R.string.spotiflac_download_page_opened
            LaunchResult.FAILED -> R.string.spotiflac_backend_missing
        }

    private fun buildTrackQuery(
        title: String,
        artists: List<String>,
    ): String =
        listOf(title, artists.joinToString(" ").trim())
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")

    private fun buildAlbumQuery(
        albumTitle: String,
        artists: List<String>,
    ): String =
        listOf(albumTitle, artists.joinToString(" ").trim(), "album")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")

    private fun copyQueryToClipboard(
        context: Context,
        query: String,
    ) {
        if (query.isBlank()) return
        context.getSystemService<ClipboardManager>()?.setPrimaryClip(
            ClipData.newPlainText("SpotiFLAC Search", query),
        )
    }

    private enum class LaunchResult {
        APP_OPENED,
        DOWNLOAD_PAGE_OPENED,
        FAILED,
    }
}
