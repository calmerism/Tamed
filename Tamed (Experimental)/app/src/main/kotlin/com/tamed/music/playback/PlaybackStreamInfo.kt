/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.playback

import com.tamed.music.db.entities.FormatEntity
import kotlin.math.roundToInt

data class PlaybackStreamInfo(
    val sourceLabel: String? = null,
    val mimeType: String,
    val codecs: String,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val bitDepth: Int? = null,
    val contentLength: Long? = null,
) {
    fun codecDisplayLabel(): String {
        val rawCodec =
            codecs
                .ifBlank { mimeType.substringAfter("/", missingDelimiterValue = mimeType) }
                .substringBefore('.')
                .trim()
                .lowercase()

        return when {
            rawCodec.contains("flac") -> "FLAC"
            rawCodec.contains("alac") -> "ALAC"
            rawCodec.contains("wav") -> "WAV"
            rawCodec.contains("aiff") -> "AIFF"
            rawCodec.contains("opus") -> "OPUS"
            rawCodec.contains("vorbis") -> "VORBIS"
            rawCodec.contains("aac") || rawCodec.contains("mp4a") -> "AAC"
            rawCodec.contains("mp3") || rawCodec.contains("mpeg") -> "MP3"
            rawCodec.isBlank() -> mimeType.substringAfter("/", missingDelimiterValue = mimeType).uppercase()
            else -> rawCodec.uppercase()
        }
    }

    fun isLossless(): Boolean =
        bitDepth?.let { it > 0 } == true ||
            codecDisplayLabel() in setOf("FLAC", "ALAC", "WAV", "AIFF")

    fun sampleRateDisplayLabel(): String? =
        sampleRate?.takeIf { it > 0 }?.let { sampleRateHz ->
            val khz = (sampleRateHz / 100.0).roundToInt() / 10.0
            "$khz kHz"
        }

    fun statsParts(): List<String> =
        buildList {
            sourceLabel?.trim()?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
            add(codecDisplayLabel())
            if (isLossless()) {
                bitDepth?.takeIf { it > 0 }?.let { add("${it}-bit") }
            } else {
                bitrate?.takeIf { it > 0 }?.let { add("${it / 1000} kbps") }
            }
            sampleRateDisplayLabel()?.let(::add)
        }

    fun statsLabel(): String = statsParts().joinToString(" • ")
}

fun FormatEntity.toPlaybackStreamInfo(sourceLabel: String? = null): PlaybackStreamInfo =
    PlaybackStreamInfo(
        sourceLabel = sourceLabel,
        mimeType = mimeType,
        codecs = codecs,
        bitrate = bitrate.takeIf { it > 0 },
        sampleRate = sampleRate,
        bitDepth = null,
        contentLength = contentLength.takeIf { it > 0L },
    )
