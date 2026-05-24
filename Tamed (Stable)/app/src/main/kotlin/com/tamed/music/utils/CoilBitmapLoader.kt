/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



 package com.tamed.music.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.tamed.music.utils.reportException
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.media3.common.util.BitmapLoader
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import coil3.size.Size
import kotlinx.coroutines.delay
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import kotlin.math.roundToInt

class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            try {
                if (data.isEmpty()) {
                    throw IllegalArgumentException("Empty image data")
                }

                BitmapFactory.decodeByteArray(data, 0, data.size)?.also { bitmap ->
                    return@future bitmap
                }

                throw IllegalStateException("Could not decode image data")
            } catch (e: Exception) {
                reportException(e)
                return@future createBitmap(64, 64)
            }
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(Size.ORIGINAL)
                    .allowHardware(false)
                    .build()

                val result = context.imageLoader.execute(request)

                when (result) {
                    is SuccessResult -> {
                        try {
                            val bitmap = result.image.toBitmap()
                            val copied = if (bitmap.isRecycled) {
                                createBitmap(64, 64)
                            } else {
                                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: createBitmap(64, 64)
                            }
                            return@future copied
                        } catch (e: Exception) {
                            reportException(e)
                        }
                    }
                    is ErrorResult -> {
                        result.throwable?.let { reportException(it) }
                    }
                }
            } catch (e: Exception) {
                reportException(e)
            }
            createBitmap(64, 64)
        }
}
