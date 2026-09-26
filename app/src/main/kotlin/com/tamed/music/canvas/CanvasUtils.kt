package com.tamed.music.canvas

import java.text.Normalizer
import java.util.Locale

fun String.normalizeForComparison(): String {
    val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
    val withoutDiacritics = Regex("\\p{InCombiningDiacriticalMarks}+").replace(decomposed, "")
    return withoutDiacritics.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun normalizeCanvasSongTitle(raw: String): String {
    val stripped =
        raw
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(
                Regex(
                    "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(Regex("\\s+"), " ")
            .trim()

    return stripped
        .trim('-')
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun normalizeCanvasArtistName(raw: String): String {
    val first =
        raw
            .split(
                Regex(
                    "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                    RegexOption.IGNORE_CASE,
                ),
                limit = 2,
            ).firstOrNull().orEmpty()

    return first.replace(Regex("\\s+"), " ").trim()
}

fun splitAndNormalizeArtists(raw: String): List<String> {
    return raw.split(
        Regex(
            "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
            RegexOption.IGNORE_CASE,
        ),
    ).map { it.normalizeForComparison() }
        .filter { it.isNotBlank() }
}

fun validateCanvasMatch(
    artwork: CanvasArtwork,
    requestedTitle: String,
    requestedArtist: String,
    requestedAlbum: String
): Boolean {
    val artist = artwork.artist
    val name = artwork.name
    val albumName = artwork.albumName

    val artistMatches = if (artist != null && requestedArtist.isNotBlank()) {
        val requestedList = splitAndNormalizeArtists(requestedArtist)
        val resultList = splitAndNormalizeArtists(artist)
        requestedList.isNotEmpty() && resultList.isNotEmpty() &&
            (requestedList.size == resultList.size && requestedList.all { req -> resultList.any { res -> res == req } } ||
             resultList.any { res -> requestedList.contains(res) } ||
             artist.normalizeForComparison().contains(requestedArtist.normalizeForComparison()) ||
             requestedArtist.normalizeForComparison().contains(artist.normalizeForComparison()))
    } else true

    val songMatches = if (name != null && requestedTitle.isNotBlank()) {
        val reqNorm = requestedTitle.normalizeForComparison()
        val reqCleanNorm = normalizeCanvasSongTitle(requestedTitle).normalizeForComparison()
        val nameNorm = name.normalizeForComparison()
        val nameCleanNorm = normalizeCanvasSongTitle(name).normalizeForComparison()
        reqNorm == nameNorm || reqCleanNorm == nameCleanNorm ||
            reqNorm.contains(nameNorm) || nameNorm.contains(reqNorm) ||
            (reqCleanNorm.isNotBlank() && reqCleanNorm == nameNorm) ||
            (nameCleanNorm.isNotBlank() && nameCleanNorm == reqNorm)
    } else true

    val albumMatches = if (albumName != null && requestedAlbum.isNotBlank()) {
        albumName.normalizeForComparison() == requestedAlbum.normalizeForComparison() ||
        albumName.normalizeForComparison().contains(requestedAlbum.normalizeForComparison()) ||
        requestedAlbum.normalizeForComparison().contains(albumName.normalizeForComparison())
    } else true

    return artistMatches && songMatches && albumMatches
}

