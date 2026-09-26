/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.together

import kotlinx.serialization.json.Json

object TogetherJson {
    val json: Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
            classDiscriminator = "type"
        }
}
