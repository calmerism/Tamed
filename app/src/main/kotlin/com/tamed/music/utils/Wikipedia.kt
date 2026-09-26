package com.tamed.music.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLQueryComponent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

object Wikipedia {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            defaultRequest { url("https://en.wikipedia.org/api/rest_v1/") }
            expectSuccess = true
        }
    }

    @Serializable
    private data class WikiSummary(val extract: String? = null)

    private suspend fun fetchPageSummary(title: String): String? = runCatching {
        client.get("page/summary/${title.replace(" ", "_").encodeURLQueryComponent()}")
            .body<WikiSummary>()
            .extract
    }.onFailure {
        if (it is ResponseException && it.response.status == HttpStatusCode.NotFound) {
            Timber.d("No Wikipedia summary found for: $title")
        } else {
            Timber.w("Failed to fetch Wikipedia summary for $title: ${it.message}")
        }
    }.getOrNull()

    suspend fun fetchAlbumInfo(albumTitle: String, artistName: String?): String? {
        if (artistName != null) {
            listOf(
                "$albumTitle ($artistName album)",
                "$albumTitle ($artistName)",
            ).forEach { query ->
                val summary = fetchPageSummary(query)
                if (summary != null && !summary.contains("may refer to", ignoreCase = true)) return summary
            }
        }

        listOf("$albumTitle (album)", albumTitle).forEach { query ->
            val summary = fetchPageSummary(query)
            if (summary != null && !summary.contains("may refer to", ignoreCase = true)) {
                if (artistName == null || summary.contains(artistName, ignoreCase = true)) return summary
            }
        }

        return null
    }
}
