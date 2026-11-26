package dev.ahmedmohamed.hayaitts.tts

/**
 * Splits request text into sections that are synthesized and streamed one at
 * a time, so audio starts after the first sentence instead of after the whole
 * request (and long requests aren't truncated).
 *
 * The first section is kept short for a fast start; the rest are sentences
 * merged up to [maxChars], with over-long sentences cut at clause marks or
 * spaces. Offsets point into the original text for `rangeStart`.
 */
object TextSections {

    data class Section(val start: Int, val end: Int, val text: String)

    private val SENTENCE_END = Regex("""(?<=[.!?…。！？；;])\s+|(?<=[。！？])|\n+""")
    private val CLAUSE = Regex("""(?<=[,，、:：—–])\s*""")

    fun split(text: String, firstMaxChars: Int = 120, maxChars: Int = 300, minChars: Int = 20): List<Section> {
        val sentences = pieces(text, 0, text.length, SENTENCE_END)
        val out = mutableListOf<Section>()
        var curStart = -1
        var curEnd = -1
        fun flush() {
            if (curStart >= 0) out += section(text, curStart, curEnd)
            curStart = -1
        }
        val spans = sentences.flatMapIndexed { i, (s, e) -> cap(text, s, e, if (i == 0) firstMaxChars else maxChars) }
        for ((s, e) in spans) {
            val limit = if (out.isEmpty()) firstMaxChars else maxChars
            if (curStart < 0) {
                curStart = s; curEnd = e
            } else if (e - curStart <= limit || curEnd - curStart < minChars) {
                curEnd = e
            } else {
                flush(); curStart = s; curEnd = e
            }
            if (out.isEmpty() && curEnd - curStart >= minChars) flush()
        }
        flush()
        return out.filter { it.text.isNotBlank() }
    }

    private fun section(text: String, s: Int, e: Int): Section {
        var a = s
        var b = e
        while (a < b && text[a].isWhitespace()) a++
        while (b > a && text[b - 1].isWhitespace()) b--
        return Section(a, b, text.substring(a, b))
    }

    private fun pieces(text: String, from: Int, to: Int, sep: Regex): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var last = from
        sep.findAll(text.substring(from, to)).forEach { m ->
            val cut = from + m.range.last + 1
            if (cut > last) out += last to cut
            last = cut
        }
        if (last < to) out += last to to
        return out
    }

    /** Cuts a span longer than [max] at clause marks, then at spaces, then hard. */
    private fun cap(text: String, s: Int, e: Int, max: Int): List<Pair<Int, Int>> {
        if (e - s <= max) return listOf(s to e)
        val out = mutableListOf<Pair<Int, Int>>()
        var start = s
        while (e - start > max) {
            val window = text.substring(start, start + max)
            val clause = CLAUSE.findAll(window).map { it.range.last + 1 }.filter { it > max / 3 }.lastOrNull()
            val space = window.lastIndexOf(' ').takeIf { it > max / 3 }
            val cut = start + (clause ?: space ?: max)
            out += start to cut
            start = cut
        }
        if (start < e) out += start to e
        return out
    }
}
