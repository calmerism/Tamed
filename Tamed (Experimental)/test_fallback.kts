import kotlin.math.max

fun tokens(s: String): Set<String> =
    s.lowercase()
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .split(Regex("\\s+"))
        .filter { it.length > 1 }
        .toSet()

fun overlapScore(a: String, b: String): Double {
    val ta = tokens(a)
    val tb = tokens(b)
    if (ta.isEmpty() || tb.isEmpty()) return 0.0
    val intersection = ta.intersect(tb).size
    val union = (ta + tb).size
    return intersection.toDouble() / union
}

fun testScore(song: String, artist: String, itemSong: String, itemArtist: String) {
    val songScore = overlapScore(song, itemSong)
    val artistScore = overlapScore(artist, itemArtist)
    val finalScore = if (songScore == 0.0) 0.0 else (0.65 * songScore + 0.35 * artistScore)
    println("Testing: $song / $artist vs $itemSong / $itemArtist")
    println("songScore: $songScore, artistScore: $artistScore, finalScore: $finalScore")
}

testScore("Deadbeat", "Tame Impala", "Dracula", "Tame Impala")
testScore("Solitude Is Bliss", "Tame Impala", "Dracula", "Tame Impala")
