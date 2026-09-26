/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.models

import androidx.compose.runtime.Immutable
import com.tamed.music.db.entities.FormatEntity
import com.tamed.music.sources.SourceKind
import java.util.Locale

@Immutable
data class ActiveSourceInfo(
    val mediaId: String,
    val sourceName: String,
    val sourceKind: SourceKind? = null,
    val codec: String? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val bitDepth: Int? = null,
    val isLossless: Boolean = false,
    val isHiRes: Boolean = false,
)

@Immutable
data class AudioQualityInfo(
    val title: String,
    val subtitle: String?,
    val codec: String,
    val bitrate: Int?,
    val sampleRate: Int?,
    val bitDepth: Int?,
    val isLossless: Boolean,
    val isHiRes: Boolean,
    val sourceName: String,
    val itag: Int? = null,
    val mimeType: String? = null,
    val channels: Int? = null,
    val loudnessDb: Double? = null,
    val contentLength: Long? = null,
) {
    companion object {
        private val LOSSLESS_CODECS = setOf("FLAC", "ALAC", "WAV", "AIFF", "APE", "WV", "DSF", "DFF")

        fun resolve(
            mediaId: String?,
            dbFormat: FormatEntity?,
            sourceInfo: ActiveSourceInfo?,
            exoFormat: androidx.media3.common.Format?,
            metadata: MediaMetadata?,
        ): AudioQualityInfo {
            // Source Name
            val resolvedSourceName = sourceInfo?.sourceName
                ?: if (metadata?.isLocalSource() == true) "Local Audio"
                else if (dbFormat?.itag != null && dbFormat.itag > 0) "YouTube Music"
                else if (dbFormat?.bitrate == 320000 || dbFormat?.codecs?.equals("mp4", ignoreCase = true) == true) "JioSaavn"
                else "YouTube Music"

            // Codec
            val rawCodec = sourceInfo?.codec
                ?: exoFormat?.codecs
                ?: exoFormat?.sampleMimeType?.substringAfter('/')
                ?: dbFormat?.codecs
                ?: dbFormat?.mimeType?.substringAfter('/')
                ?: if (metadata?.isLosslessSource() == true) "FLAC" else "OPUS"

            val cleanCodec = when {
                rawCodec.contains("flac", ignoreCase = true) -> "FLAC"
                rawCodec.contains("alac", ignoreCase = true) -> "ALAC"
                rawCodec.contains("opus", ignoreCase = true) -> "OPUS"
                rawCodec.contains("mp4a", ignoreCase = true) || rawCodec.contains("aac", ignoreCase = true) -> "AAC"
                rawCodec.contains("mp4", ignoreCase = true) -> "AAC"
                rawCodec.contains("wav", ignoreCase = true) -> "WAV"
                rawCodec.contains("mpeg", ignoreCase = true) || rawCodec.contains("mp3", ignoreCase = true) -> "MP3"
                rawCodec.contains("vorbis", ignoreCase = true) -> "Vorbis"
                resolvedSourceName == "JioSaavn" -> "AAC"
                else -> rawCodec.uppercase(Locale.ROOT)
            }

            // Bitrate
            val bitrate = sourceInfo?.bitrate
                ?: exoFormat?.bitrate?.takeIf { it > 0 }
                ?: dbFormat?.bitrate?.takeIf { it > 0 }
                ?: if (cleanCodec == "OPUS") 160000 else null

            // Sample Rate
            val sampleRate = sourceInfo?.sampleRate
                ?: exoFormat?.sampleRate?.takeIf { it > 0 }
                ?: dbFormat?.sampleRate?.takeIf { it > 0 }
                ?: 44100

            // Bit Depth
            val bitDepth = sourceInfo?.bitDepth
                ?: when (exoFormat?.pcmEncoding) {
                    androidx.media3.common.C.ENCODING_PCM_24BIT -> 24
                    androidx.media3.common.C.ENCODING_PCM_32BIT, androidx.media3.common.C.ENCODING_PCM_FLOAT -> 32
                    androidx.media3.common.C.ENCODING_PCM_16BIT -> 16
                    else -> null
                }

            // Lossless check
            val isLossless = sourceInfo?.isLossless == true
                || metadata?.isLosslessSource() == true
                || cleanCodec in LOSSLESS_CODECS
                || (exoFormat?.sampleMimeType?.lowercase(Locale.ROOT) in setOf("audio/flac", "audio/alac", "audio/wav", "audio/x-wav"))

            val isHiRes = isLossless && ((bitDepth ?: 16) > 16 || sampleRate > 48000)

            // Badge Title - Codec names: "AAC", "OPUS", "Lossless"
            val title = when {
                isLossless -> "Lossless"
                cleanCodec.isNotBlank() -> cleanCodec
                else -> "AAC"
            }

            // Subtitle
            val subtitle = if (isLossless) {
                val parts = mutableListOf(cleanCodec)
                if (bitDepth != null) parts.add("$bitDepth-bit")
                parts.add("${"%.1f".format(Locale.ROOT, sampleRate / 1000f).removeSuffix(".0")} kHz")
                parts.joinToString(" · ")
            } else {
                val parts = mutableListOf(cleanCodec)
                if (bitrate != null && bitrate > 0) parts.add("${bitrate / 1000} kbps")
                parts.joinToString(" · ")
            }

            return AudioQualityInfo(
                title = title,
                subtitle = subtitle,
                codec = cleanCodec,
                bitrate = bitrate,
                sampleRate = sampleRate,
                bitDepth = bitDepth,
                isLossless = isLossless,
                isHiRes = isHiRes,
                sourceName = resolvedSourceName,
                itag = dbFormat?.itag,
                mimeType = dbFormat?.mimeType ?: exoFormat?.sampleMimeType,
                channels = exoFormat?.channelCount ?: 2,
                loudnessDb = dbFormat?.loudnessDb,
                contentLength = dbFormat?.contentLength,
            )
        }
    }
}
