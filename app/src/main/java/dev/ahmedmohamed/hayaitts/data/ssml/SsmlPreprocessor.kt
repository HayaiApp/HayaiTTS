package dev.ahmedmohamed.hayaitts.data.ssml

/**
 * Minimal SSML subset parser. Produces a stream of [Segment]s the TTS service
 * can render sequentially — each segment carries its own prosody overrides on
 * top of the caller-supplied defaults.
 *
 * Supported tags (case-insensitive):
 *   - `<speak>` — wrapper; stripped.
 *   - `<prosody rate="0.8" pitch="1.2">…</prosody>` — multiplies the
 *     segment's `rate`/`pitch` relative to the caller default.
 *   - `<break time="400ms"/>` or `<break time="1s"/>` — inserts silence.
 *   - `<say-as interpret-as="characters">abc</say-as>` — spells out.
 *
 * Anything else parses as plain text. We deliberately reject DOCTYPE / xmlns
 * attributes to keep the input shape small; richer SSML is out of scope for
 * v2.0.0-b1.
 *
 * The parser is a regex sweep over the input, not a real XML parser. Good
 * enough for hand-authored test snippets and the Studio editor; rich docs
 * should go through a real lexer.
 */
class SsmlPreprocessor {

    data class Segment(
        val text: String,
        val rateMultiplier: Float = 1f,
        val pitchMultiplier: Float = 1f,
        /** Trailing silence in ms after this segment. */
        val breakAfterMs: Long = 0L,
        val spellOut: Boolean = false,
    )

    fun parse(input: String): List<Segment> {
        val stripped = input
            .replace(Regex("""<speak[^>]*>"""), "")
            .replace("</speak>", "")
            .trim()
        if (stripped.isEmpty()) return emptyList()

        val out = mutableListOf<Segment>()
        var cursor = 0
        while (cursor < stripped.length) {
            val nextTag = TAG_OPEN.find(stripped, cursor)
            if (nextTag == null) {
                val tail = stripped.substring(cursor)
                if (tail.isNotBlank()) out += Segment(text = tail.trim())
                break
            }
            val before = stripped.substring(cursor, nextTag.range.first)
            if (before.isNotBlank()) out += Segment(text = before.trim())
            val tagName = nextTag.groupValues[1].lowercase()
            cursor = when (tagName) {
                "prosody" -> handleProsody(stripped, nextTag, out)
                "break" -> handleBreak(stripped, nextTag, out)
                "say-as" -> handleSayAs(stripped, nextTag, out)
                else -> nextTag.range.last + 1  // unknown tag → skip the opener
            }
        }
        return out
    }

    private fun handleProsody(
        text: String,
        opener: MatchResult,
        out: MutableList<Segment>,
    ): Int {
        val attrs = opener.groupValues[2]
        val rate = ATTR_RATE.find(attrs)?.groupValues?.get(1)?.toFloatOrNull() ?: 1f
        val pitch = ATTR_PITCH.find(attrs)?.groupValues?.get(1)?.toFloatOrNull() ?: 1f
        val end = TAG_CLOSE_PROSODY.find(text, opener.range.last + 1)
        val bodyStart = opener.range.last + 1
        val bodyEnd = end?.range?.first ?: text.length
        val inner = text.substring(bodyStart, bodyEnd).trim()
        if (inner.isNotEmpty()) {
            out += Segment(text = inner, rateMultiplier = rate, pitchMultiplier = pitch)
        }
        return (end?.range?.last ?: (text.length - 1)) + 1
    }

    private fun handleBreak(
        text: String,
        opener: MatchResult,
        out: MutableList<Segment>,
    ): Int {
        val attrs = opener.groupValues[2]
        val timeStr = ATTR_TIME.find(attrs)?.groupValues?.get(1).orEmpty()
        val ms = parseDurationMs(timeStr)
        if (out.isEmpty()) {
            out += Segment(text = "", breakAfterMs = ms)
        } else {
            val last = out.removeAt(out.lastIndex)
            out += last.copy(breakAfterMs = last.breakAfterMs + ms)
        }
        return opener.range.last + 1
    }

    private fun handleSayAs(
        text: String,
        opener: MatchResult,
        out: MutableList<Segment>,
    ): Int {
        val attrs = opener.groupValues[2]
        val interpret = ATTR_INTERPRET.find(attrs)?.groupValues?.get(1).orEmpty()
        val end = TAG_CLOSE_SAYAS.find(text, opener.range.last + 1)
        val bodyStart = opener.range.last + 1
        val bodyEnd = end?.range?.first ?: text.length
        val inner = text.substring(bodyStart, bodyEnd).trim()
        if (inner.isNotEmpty()) {
            val spell = interpret.equals("characters", ignoreCase = true) ||
                interpret.equals("spell-out", ignoreCase = true)
            val rendered = if (spell) inner.toCharArray().joinToString(" ") else inner
            out += Segment(text = rendered, spellOut = spell)
        }
        return (end?.range?.last ?: (text.length - 1)) + 1
    }

    private fun parseDurationMs(raw: String): Long {
        if (raw.isEmpty()) return 0L
        val m = DURATION.matchEntire(raw.trim()) ?: return 0L
        val value = m.groupValues[1].toFloatOrNull() ?: return 0L
        return when (m.groupValues[2].lowercase()) {
            "s" -> (value * 1000f).toLong()
            "ms", "" -> value.toLong()
            else -> 0L
        }
    }

    companion object {
        private val TAG_OPEN = Regex("""<(prosody|break|say-as)([^>]*)>""", RegexOption.IGNORE_CASE)
        private val TAG_CLOSE_PROSODY = Regex("""</prosody>""", RegexOption.IGNORE_CASE)
        private val TAG_CLOSE_SAYAS = Regex("""</say-as>""", RegexOption.IGNORE_CASE)
        private val ATTR_RATE = Regex("""rate\s*=\s*"([^"]+)"""")
        private val ATTR_PITCH = Regex("""pitch\s*=\s*"([^"]+)"""")
        private val ATTR_TIME = Regex("""time\s*=\s*"([^"]+)"""")
        private val ATTR_INTERPRET = Regex("""interpret-as\s*=\s*"([^"]+)"""")
        private val DURATION = Regex("""(\d+(?:\.\d+)?)\s*(ms|s)?""", RegexOption.IGNORE_CASE)
    }
}
