package com.tamed.music.applecanvas

import com.tamed.music.canvas.CanvasArtwork
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object MonochromeApiCanvas {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getForSong(title: String, artist: String, album: String?): CanvasArtwork? {
        return try {
            val query = if (album != null) "$title $artist $album" else "$title $artist"
            val response = client.get("https://canvas.monochrome.workers.dev/search?q=${query.encodeURL()}")
            if (response.status.value == 200) {
                val body = response.bodyAsText()
                val results = json.parseToJsonElement(body).jsonArray
                if (results.isNotEmpty()) {
                    val first = results[0].jsonObject
                    val resName = first["name"]?.jsonPrimitive?.content ?: ""
                    val resArtist = first["artist"]?.jsonPrimitive?.content ?: ""
                    val resAlbum = first["albumName"]?.jsonPrimitive?.content ?: ""
                    
                    // Basic validation
                    val nameMatch = resName.contains(title, ignoreCase = true) || title.contains(resName, ignoreCase = true)
                    val artistMatch = resArtist.contains(artist, ignoreCase = true) || artist.contains(resArtist, ignoreCase = true)
                    val albumMatch = album == null || resAlbum.contains(album, ignoreCase = true) || (album.isNotBlank() && album.contains(resAlbum, ignoreCase = true))
                    
                    if ((nameMatch || albumMatch) && artistMatch) {
                        CanvasArtwork(
                            name = resName,
                            artist = resArtist,
                            albumName = resAlbum,
                            static = first["static"]?.jsonPrimitive?.content,
                            animated = first["animated"]?.jsonPrimitive?.content,
                            videoUrl = first["videoUrl"]?.jsonPrimitive?.content
                        )
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun String.encodeURL(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
