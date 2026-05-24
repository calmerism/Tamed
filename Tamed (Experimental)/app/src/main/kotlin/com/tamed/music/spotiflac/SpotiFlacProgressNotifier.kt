/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.spotiflac

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tamed.music.R

object SpotiFlacProgressNotifier {
    private const val CHANNEL_ID = "spotiflac_download_progress"

    private fun notificationId(itemId: String): Int =
        (itemId.hashCode() and 0x7fffffff).coerceAtLeast(1) + 0x5000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.spotiflac_download_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.spotiflac_download_channel_desc)
            }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        itemId: String,
        title: String,
        subtitle: String,
        progress: Float? = null,
        status: String? = null,
    ) {
        ensureChannel(context)
        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.download)
                .setContentTitle(context.getString(R.string.spotiflac_downloading))
                .setContentText(
                    when {
                        status.equals("finalizing", ignoreCase = true) -> context.getString(R.string.spotiflac_finalizing_download)
                        subtitle.isNotBlank() -> subtitle
                        else -> title
                    },
                )
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        listOf(title, subtitle).filter { it.isNotBlank() }.joinToString("\n"),
                    ),
                )
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)

        val percent = progress?.coerceIn(0f, 1f)
        if (percent == null || percent <= 0f) {
            builder.setProgress(100, 0, true)
        } else {
            builder.setProgress(100, (percent * 100).toInt().coerceIn(0, 100), percent < 1f)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId(itemId), builder.build())
        } catch (_: SecurityException) {
            // Notifications may be disabled or unavailable on this device.
        }
    }

    fun cancel(
        context: Context,
        itemId: String,
    ) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId(itemId))
        } catch (_: SecurityException) {
            // Ignore.
        }
    }

    fun showFailed(
        context: Context,
        itemId: String,
        title: String,
        subtitle: String,
        errorMsg: String,
    ) {
        ensureChannel(context)
        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.download) // Using the same download icon for consistency
                .setContentTitle(context.getString(R.string.spotiflac_download_failed))
                .setContentText(title)
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        listOf(title, subtitle, errorMsg).filter { it.isNotBlank() }.joinToString("\n"),
                    ),
                )
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        try {
            NotificationManagerCompat.from(context).notify(notificationId(itemId), builder.build())
        } catch (_: SecurityException) {
            // Notifications may be disabled or unavailable on this device.
        }
    }
}
