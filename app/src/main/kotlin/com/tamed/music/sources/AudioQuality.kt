package com.tamed.music.sources

import kotlinx.coroutines.flow.MutableStateFlow

enum class AudioQuality(
    val maxKbps: Int,
    val label: String,
    val detail: String,
    val hourly: String,
) {
    LOW(64, "Low", "~64 kbps", "29 MB/hr"),
    MEDIUM(Int.MAX_VALUE, "Medium", "~171 kbps", "77 MB/hr"),
    HIGH(Int.MAX_VALUE, "High", "320 kbps", "144 MB/hr"),
    LOSSLESS(Int.MAX_VALUE, "Lossless", "FLAC / ALAC · Bit-exact", "300+ MB/hr"),
    ;

    fun permits(kind: SourceKind): Boolean = when (this) {
        LOSSLESS -> true
        HIGH -> !kind.canServeLossless
        MEDIUM, LOW -> kind == SourceKind.YOUTUBE
    }

    companion object {
        val AUTO get() = MEDIUM
        val HIGHEST get() = LOSSLESS

        fun fromName(name: String?): AudioQuality = when (name?.uppercase()) {
            "LOSSLESS", "HIGHEST" -> LOSSLESS
            "HIGH" -> HIGH
            "MEDIUM", "AUTO" -> MEDIUM
            "LOW" -> LOW
            else -> LOSSLESS
        }
    }
}

object SourceSettings {
    val audioQuality = MutableStateFlow(AudioQuality.LOSSLESS)
    val effectiveAudioQuality: AudioQuality get() = audioQuality.value
    val dolbyAtmos = MutableStateFlow(true)
}
