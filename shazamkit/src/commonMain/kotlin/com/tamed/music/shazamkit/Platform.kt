package com.tamed.music.shazamkit

import kotlin.random.Random

internal expect fun currentTimeMillis(): Long

internal fun randomUuid(): String {
    val bytes = ByteArray(16)
    Random.nextBytes(bytes)
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
    val hex = bytes.joinToString("") { byte ->
        (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
    }
    return buildString {
        append(hex.substring(0, 8))
        append('-')
        append(hex.substring(8, 12))
        append('-')
        append(hex.substring(12, 16))
        append('-')
        append(hex.substring(16, 20))
        append('-')
        append(hex.substring(20, 32))
    }
}
