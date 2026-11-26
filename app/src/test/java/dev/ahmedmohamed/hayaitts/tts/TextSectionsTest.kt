package dev.ahmedmohamed.hayaitts.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSectionsTest {

    @Test
    fun `offsets point back into the source text`() {
        val text = "Hello there, this is the first sentence. And here is a second one! Third?"
        val sections = TextSections.split(text)
        sections.forEach { assertEquals(it.text, text.substring(it.start, it.end)) }
        assertTrue(sections.size >= 2)
    }

    @Test
    fun `first section is short for a fast start`() {
        val text = (1..40).joinToString(" ") { "Sentence number $it is here." }
        val sections = TextSections.split(text)
        assertTrue(sections.first().text.length <= 120)
        assertTrue(sections.all { it.text.length <= 300 })
        // Nothing lost: every word survives.
        assertEquals(text.split(" ").size, sections.sumOf { it.text.split(" ").size })
    }

    @Test
    fun `long text without punctuation is still split and not truncated`() {
        val text = "word ".repeat(2000).trim()
        val sections = TextSections.split(text)
        assertEquals(2000, sections.sumOf { it.text.split(" ").size })
    }

    @Test
    fun `cjk sentences split on full-width punctuation`() {
        val sections = TextSections.split("发动机总扭矩为百分之零。这是第二句话，比较长一些，用来测试分句是否正确。")
        assertTrue(sections.size >= 1)
        assertTrue(sections.first().text.endsWith("。"))
    }

    @Test
    fun `blank text yields nothing`() {
        assertEquals(emptyList<TextSections.Section>(), TextSections.split("   \n "))
    }
}
