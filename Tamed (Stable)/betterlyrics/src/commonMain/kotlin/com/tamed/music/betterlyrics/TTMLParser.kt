/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.betterlyrics

object TTMLParser {
    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val words: List<ParsedWord>,
        val isBackground: Boolean = false,
        val agent: String? = null,
    )

    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val isBackground: Boolean = false,
    )

    private data class TimingContext(
        val tickRate: Double,
        val frameRate: Double,
    )

    private data class XmlNode(
        val name: String?,
        val attributes: Map<String, String> = emptyMap(),
        val children: MutableList<XmlNode> = mutableListOf(),
        val text: String? = null,
    ) {
        val isText: Boolean
            get() = name == null
    }

    fun parseTTML(ttml: String): List<ParsedLine> {
        val root = parseXml(ttml) ?: return emptyList()
        val ttNode = findFirstElement(root) { it.localName() == "tt" } ?: root
        val timingContext = readTimingContext(ttNode)
        val pNodes = mutableListOf<XmlNode>()
        collectElements(root, pNodes) { it.localName() == "p" }

        return pNodes.mapNotNull { parseLine(it, timingContext) }.sortedBy { it.startTime }
    }

    private fun parseLine(
        pElement: XmlNode,
        timingContext: TimingContext,
    ): ParsedLine? {
        val begin = pElement.attributeBySuffix("begin") ?: return null
        val end = pElement.attributeBySuffix("end")
        val dur = pElement.attributeBySuffix("dur")

        val startTime = parseTime(begin, timingContext)
        val endTime = when {
            !end.isNullOrBlank() -> parseTime(end, timingContext)
            !dur.isNullOrBlank() -> startTime + parseTime(dur, timingContext)
            else -> startTime + 5.0
        }

        val agent = pElement.attributeBySuffix("agent")?.takeIf { it.isNotBlank() }
        val words = mutableListOf<ParsedWord>()
        val lineText = StringBuilder()

        parseSpanElements(
            element = pElement,
            words = words,
            lineText = lineText,
            lineStartTime = startTime,
            lineEndTime = endTime,
            isBackground = false,
            timingContext = timingContext,
        )

        if (words.isEmpty() && lineText.isNotEmpty()) {
            words += fallbackWords(
                text = lineText.toString(),
                startTime = startTime,
                endTime = endTime,
                isBackground = false,
            )
        } else if (lineText.isEmpty()) {
            val directText = getDirectTextContent(pElement).trim()
            if (directText.isNotEmpty()) {
                lineText.append(directText)
                words += fallbackWords(
                    text = directText,
                    startTime = startTime,
                    endTime = endTime,
                    isBackground = false,
                )
            }
        }

        val finalText = lineText.toString().trim()
        if (finalText.isEmpty()) return null

        return ParsedLine(
            text = finalText,
            startTime = startTime,
            endTime = endTime,
            words = words,
            isBackground = false,
            agent = agent,
        )
    }

    private fun parseSpanElements(
        element: XmlNode,
        words: MutableList<ParsedWord>,
        lineText: StringBuilder,
        lineStartTime: Double,
        lineEndTime: Double,
        isBackground: Boolean,
        timingContext: TimingContext,
    ) {
        element.children.forEach { child ->
            if (child.isText) {
                val text = child.text.orEmpty()
                if (text.isNotBlank()) {
                    lineText.append(text)
                } else if (text.isNotEmpty() && !text.contains('\n')) {
                    if (words.isNotEmpty() && !words.last().text.endsWith(" ")) {
                        lineText.append(" ")
                        val lastWord = words.last()
                        words[words.lastIndex] = lastWord.copy(text = lastWord.text + " ")
                    }
                }
                return@forEach
            }

            if (child.localName() != "span") {
                parseSpanElements(child, words, lineText, lineStartTime, lineEndTime, isBackground, timingContext)
                return@forEach
            }

            val role = child.attributeBySuffix("role")
            val isBgSpan = role == "x-bg" || isBackground
            val wordBegin = child.attributeBySuffix("begin")
            val wordEnd = child.attributeBySuffix("end")
            val wordDur = child.attributeBySuffix("dur")

            if (hasDirectSpanChildren(child)) {
                parseSpanElements(child, words, lineText, lineStartTime, lineEndTime, isBgSpan, timingContext)
            } else {
                val wordText = getDirectTextContent(child)
                if (wordText.isEmpty()) return@forEach

                val isSyllableContinuation = words.isNotEmpty() && !words.last().text.endsWith(" ")
                lineText.append(wordText)

                val rawWordStart = wordBegin?.takeIf { it.isNotBlank() }?.let { parseTime(it, timingContext) }
                val rawWordEnd = when {
                    !wordEnd.isNullOrBlank() -> parseTime(wordEnd, timingContext)
                    !wordDur.isNullOrBlank() && rawWordStart != null -> rawWordStart + parseTime(wordDur, timingContext)
                    else -> null
                }

                val wordStartTime = normalizeChildTime(
                    raw = rawWordStart,
                    lineStartTime = lineStartTime,
                    lineEndTime = lineEndTime,
                    fallback = lineStartTime,
                )
                val wordEndTime = normalizeChildTime(
                    raw = rawWordEnd,
                    lineStartTime = lineStartTime,
                    lineEndTime = lineEndTime,
                    fallback = lineEndTime,
                ).coerceAtLeast(wordStartTime)

                val trimmedText = wordText.trim()
                val newWord = ParsedWord(
                    text = trimmedText,
                    startTime = wordStartTime,
                    endTime = wordEndTime,
                    isBackground = isBgSpan,
                )

                val lastWord = words.lastOrNull()
                if (
                    isSyllableContinuation &&
                    lastWord != null &&
                    !lastWord.text.endsWith(" ") &&
                    lastWord.isBackground == isBgSpan &&
                    !containsCjk(lastWord.text.trim()) &&
                    !containsCjk(trimmedText) &&
                    trimmedText.isNotEmpty()
                ) {
                    words[words.lastIndex] = lastWord.copy(
                        text = lastWord.text + trimmedText,
                        endTime = wordEndTime,
                    )
                } else if (trimmedText.isNotEmpty()) {
                    words.add(newWord)
                }
            }
        }
    }

    private fun fallbackWords(
        text: String,
        startTime: Double,
        endTime: Double,
        isBackground: Boolean,
    ): List<ParsedWord> {
        val tokens = splitWords(text)
        if (tokens.isEmpty()) return emptyList()

        val totalDuration = endTime - startTime
        val totalLength = tokens.sumOf { it.length }.toDouble().coerceAtLeast(1.0)
        var currentWordStart = startTime

        return buildList {
            tokens.forEach { token ->
                val wordDuration = (token.length / totalLength) * totalDuration
                val wordEnd = currentWordStart + wordDuration
                add(
                    ParsedWord(
                        text = token,
                        startTime = currentWordStart,
                        endTime = wordEnd,
                        isBackground = isBackground,
                    ),
                )
                currentWordStart = wordEnd
            }
        }
    }

    private fun splitWords(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        if (containsCjk(text)) {
            val pieces = mutableListOf<String>()
            val current = StringBuilder()
            text.forEach { char ->
                when {
                    char.isWhitespace() -> {
                        if (current.isNotEmpty()) {
                            pieces += current.toString()
                            current.clear()
                        }
                        if (pieces.isNotEmpty()) {
                            pieces[pieces.lastIndex] = pieces.last() + char
                        }
                    }
                    isCjkChar(char) -> {
                        if (current.isNotEmpty()) {
                            pieces += current.toString()
                            current.clear()
                        }
                        pieces += char.toString()
                    }
                    else -> current.append(char)
                }
            }
            if (current.isNotEmpty()) pieces += current.toString()
            return pieces
        }

        val matches = Regex("""\S+\s*""").findAll(text)
        return matches.map { it.value }.toList()
    }

    private fun containsCjk(text: String): Boolean = text.any(::isCjkChar)

    private fun isCjkChar(char: Char): Boolean {
        val code = char.code
        return code in 0x4E00..0x9FFF ||
            code in 0x3400..0x4DBF ||
            code in 0xF900..0xFAFF ||
            code in 0x3040..0x309F ||
            code in 0x30A0..0x30FF ||
            code in 0xAC00..0xD7AF ||
            code in 0x1100..0x11FF ||
            code in 0x3130..0x318F
    }

    private fun getDirectTextContent(element: XmlNode): String {
        return buildString {
            element.children.forEach { child ->
                if (child.isText) {
                    append(child.text.orEmpty())
                }
            }
        }
    }

    private fun hasDirectSpanChildren(element: XmlNode): Boolean {
        return element.children.any { !it.isText && it.localName() == "span" }
    }

    private fun normalizeChildTime(
        raw: Double?,
        lineStartTime: Double,
        lineEndTime: Double,
        fallback: Double,
    ): Double {
        if (raw == null || raw.isNaN() || raw.isInfinite()) return fallback
        val lineDuration = (lineEndTime - lineStartTime).coerceAtLeast(0.0)
        val isProbablyRelative = raw < (lineStartTime - 0.25) && raw <= (lineDuration + 1.0)
        val adjusted = if (isProbablyRelative) lineStartTime + raw else raw
        return adjusted.coerceIn(lineStartTime.coerceAtLeast(0.0), lineEndTime.coerceAtLeast(lineStartTime))
    }

    private fun readTimingContext(root: XmlNode): TimingContext {
        fun attr(suffix: String): String? = root.attributeBySuffix(suffix)?.trim()?.takeIf { it.isNotEmpty() }

        val baseFrameRate = attr("frameRate")?.toDoubleOrNull() ?: 30.0
        val frameRateMultiplier = attr("frameRateMultiplier")
            ?.split(Regex("""\s+"""))
            ?.mapNotNull { it.toDoubleOrNull() }
            ?.takeIf { it.size == 2 && it[1] != 0.0 }
            ?.let { it[0] / it[1] }
            ?: 1.0
        val frameRate = (baseFrameRate * frameRateMultiplier).coerceAtLeast(1.0)
        val tickRate = attr("tickRate")?.toDoubleOrNull() ?: frameRate.coerceAtLeast(1.0)

        return TimingContext(
            tickRate = tickRate,
            frameRate = frameRate,
        )
    }

    private fun parseTime(
        timeStr: String,
        timingContext: TimingContext,
    ): Double {
        return try {
            val raw = timeStr.trim()
            if (raw.isEmpty()) return 0.0

            val offsetMatch =
                Regex("""^([0-9]+(?:\.[0-9]+)?)(h|ms|m|s|f|t)$""", RegexOption.IGNORE_CASE)
                    .matchEntire(raw)
            if (offsetMatch != null) {
                val value = offsetMatch.groupValues[1].toDoubleOrNull() ?: return 0.0
                return when (offsetMatch.groupValues[2].lowercase()) {
                    "h" -> value * 3600.0
                    "m" -> value * 60.0
                    "s" -> value
                    "ms" -> value / 1000.0
                    "f" -> value / timingContext.frameRate
                    "t" -> value / timingContext.tickRate
                    else -> value
                }
            }

            val cleanClock = raw.replace(';', ':').trimEnd { it.isLetter() }
            if (cleanClock.contains(":")) {
                val parts = cleanClock.split(":")
                return when (parts.size) {
                    2 -> (parts[0].toDoubleOrNull() ?: 0.0) * 60.0 + (parts[1].toDoubleOrNull() ?: 0.0)
                    3 -> {
                        val hours = parts[0].toDoubleOrNull() ?: 0.0
                        val minutes = parts[1].toDoubleOrNull() ?: 0.0
                        val seconds = parts[2].toDoubleOrNull() ?: 0.0
                        hours * 3600.0 + minutes * 60.0 + seconds
                    }
                    4 -> {
                        val hours = parts[0].toDoubleOrNull() ?: 0.0
                        val minutes = parts[1].toDoubleOrNull() ?: 0.0
                        val seconds = parts[2].toDoubleOrNull() ?: 0.0
                        val frames = parts[3].toDoubleOrNull() ?: 0.0
                        hours * 3600.0 + minutes * 60.0 + seconds + (frames / timingContext.frameRate)
                    }
                    else -> cleanClock.toDoubleOrNull() ?: 0.0
                }
            }

            raw.toDoubleOrNull() ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    private fun parseXml(xml: String): XmlNode? {
        val tokenRegex = Regex("""(?s)<!--.*?-->|<!\[CDATA\[.*?]]>|<\?.*?\?>|</?[^>]+>|[^<]+""")
        val root = XmlNode(name = "__root__")
        val stack = mutableListOf(root)

        tokenRegex.findAll(xml).forEach { match ->
            val token = match.value
            when {
                token.startsWith("<!--") || token.startsWith("<?") -> Unit
                token.startsWith("<![CDATA[") -> {
                    stack.last().children += XmlNode(name = null, text = token.removePrefix("<![CDATA[").removeSuffix("]]>"))
                }
                token.startsWith("</") -> {
                    val closingName = token.removePrefix("</").removeSuffix(">").trim().substringBeforeWhitespace()
                    while (stack.size > 1 && !namesMatch(stack.last().name, closingName)) {
                        stack.removeAt(stack.lastIndex)
                    }
                    if (stack.size > 1) {
                        stack.removeAt(stack.lastIndex)
                    }
                }
                token.startsWith("<") -> {
                    val selfClosing = token.endsWith("/>")
                    val inner = token.removePrefix("<").removeSuffix(if (selfClosing) "/>" else ">").trim()
                    if (!inner.startsWith("!")) {
                        val tagName = inner.substringBeforeWhitespace()
                        val attributes = parseAttributes(inner.removePrefix(tagName))
                        val node = XmlNode(name = tagName, attributes = attributes)
                        stack.last().children += node
                        if (!selfClosing) {
                            stack += node
                        }
                    }
                }
                else -> {
                    val decoded = decodeXmlEntities(token)
                    if (decoded.isNotEmpty()) {
                        stack.last().children += XmlNode(name = null, text = decoded)
                    }
                }
            }
        }

        return root
    }

    private fun parseAttributes(raw: String): Map<String, String> {
        val attributeRegex = Regex("""([:\w-]+)\s*=\s*("([^"]*)"|'([^']*)')""")
        return buildMap {
            attributeRegex.findAll(raw).forEach { match ->
                val key = match.groupValues[1]
                val value = match.groupValues[3].ifEmpty { match.groupValues[4] }
                put(key, decodeXmlEntities(value))
            }
        }
    }

    private fun decodeXmlEntities(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    private fun findFirstElement(
        node: XmlNode,
        predicate: (XmlNode) -> Boolean,
    ): XmlNode? {
        node.children.forEach { child ->
            if (!child.isText) {
                if (predicate(child)) return child
                findFirstElement(child, predicate)?.let { return it }
            }
        }
        return null
    }

    private fun collectElements(
        node: XmlNode,
        result: MutableList<XmlNode>,
        predicate: (XmlNode) -> Boolean,
    ) {
        node.children.forEach { child ->
            if (!child.isText) {
                if (predicate(child)) {
                    result += child
                }
                collectElements(child, result, predicate)
            }
        }
    }

    private fun XmlNode.localName(): String = name?.substringAfterLast(':')?.lowercase().orEmpty()

    private fun XmlNode.attributeBySuffix(suffix: String): String? {
        return attributes.entries.firstOrNull { it.key.substringAfterLast(':').equals(suffix, ignoreCase = true) }?.value
    }

    private fun namesMatch(
        left: String?,
        right: String?,
    ): Boolean {
        if (left == null || right == null) return false
        return left.substringAfterLast(':').equals(right.substringAfterLast(':'), ignoreCase = true)
    }

    private fun String.substringBeforeWhitespace(): String {
        val index = indexOfFirst { it.isWhitespace() }
        return if (index == -1) this else substring(0, index)
    }
}
