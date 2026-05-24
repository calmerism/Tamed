package com.tamed.music.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

object AppleMusicAboutAlbum {
    private const val APPLE_MUSIC_TOKEN =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IldlYlBsYXlLaWQifQ" +
            ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzc0NDU2MzgyLCJleHAiOjE3ODE3" +
            "MTM5ODIsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
            ".4n8qYF4qa18sL1E0G9A3qX35cD8wQ-IJcS9Bh8ZT8JV_yLBtVq46B-9-2ZS3EvWHuw3yK9BYFYAhAdTaDm38vQ"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 25_000
                socketTimeoutMillis = 25_000
            }
            expectSuccess = false
        }
    }

    suspend fun fetchAlbumDescription(
        albumTitle: String,
        artistName: String?,
        storefront: String = "us",
    ): String? = runCatching {
        val query = if (artistName != null && !albumTitle.contains(artistName, ignoreCase = true)) {
            "$artistName $albumTitle"
        } else {
            albumTitle
        }

        val response = client.get("https://amp-api.music.apple.com/v1/catalog/$storefront/search") {
            header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
            header("Origin", "https://music.apple.com")
            header("Referer", "https://music.apple.com/")
            parameter("term", query)
            parameter("types", "albums")
            parameter("limit", "5")
            parameter("extend", "editorialNotes")
        }
        if (response.status != HttpStatusCode.OK) return@runCatching null

        val albums = response.body<kotlinx.serialization.json.JsonObject>()
            .get("results")?.jsonObject
            ?.get("albums")?.jsonObject
            ?.get("data")?.jsonArray
            ?: return@runCatching null

        val attributes = albums.mapNotNull { item ->
            val attr = item.jsonObject["attributes"]?.jsonObject ?: return@mapNotNull null
            val resultArtistName = attr["artistName"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val resultName = attr["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            var score = 0
            if (artistName != null) {
                if (resultArtistName.equals(artistName, ignoreCase = true)) score += 10
                else if (resultArtistName.contains(artistName, ignoreCase = true) || artistName.contains(resultArtistName, ignoreCase = true)) score += 5
            }
            if (resultName.equals(albumTitle, ignoreCase = true)) score += 10
            else if (resultName.contains(albumTitle, ignoreCase = true) || albumTitle.contains(resultName, ignoreCase = true)) score += 5
            score to attr
        }.sortedByDescending { it.first }.firstOrNull { it.first >= 10 }?.second ?: return@runCatching null

        val notes = attributes["editorialNotes"]?.jsonObject
        (notes?.get("standard")?.jsonPrimitive?.contentOrNull
            ?: notes?.get("short")?.jsonPrimitive?.contentOrNull)
            ?.replace(Regex("<[^>]*>"), "")
            ?.trim()
    }.onFailure {
        Timber.w("Failed to fetch Apple Music description for $albumTitle: ${it.message}")
    }.getOrNull()
}
