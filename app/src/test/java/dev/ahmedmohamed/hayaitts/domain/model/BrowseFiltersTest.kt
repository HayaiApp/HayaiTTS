package dev.ahmedmohamed.hayaitts.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseFiltersTest {

    private val piperAmyFemale = VoiceCard(
        id = "en_US-amy-medium", family = "piper", title = "Amy",
        languages = listOf("en-US"),
        speakers = listOf(Speaker(0, "amy", "F", "inferred")),
        sampleRateHz = 22050, approxSizeMb = 28, tier = "mid",
        license = "MIT", bundleUrl = "", available = true,
    )
    private val piperRyanMale = piperAmyFemale.copy(
        id = "en_US-ryan-high", title = "Ryan",
        speakers = listOf(Speaker(0, "ryan", "M", "inferred")),
        tier = "high", approxSizeMb = 75,
    )
    private val kokoroMulti = piperAmyFemale.copy(
        id = "kokoro-v1.1", family = "kokoro", title = "Kokoro",
        speakers = listOf(
            Speaker(0, "af_alloy", "F", "declared"),
            Speaker(1, "am_adam", "M", "declared"),
        ),
        sampleRateHz = 24000, approxSizeMb = 320, tier = "high", license = "Apache-2.0",
    )
    private val unknownVoice = piperAmyFemale.copy(
        id = "vits-unknown", family = "vits",
        speakers = listOf(Speaker(0, "speaker_0", "U", "unknown")),
    )

    private val catalog = listOf(piperAmyFemale, piperRyanMale, kokoroMulti, unknownVoice)

    @Test
    fun `empty filters returns full list`() {
        assertEquals(catalog, catalog.applyFilters(BrowseFilters()))
    }

    @Test
    fun `gender female matches inferred and declared female`() {
        val r = catalog.applyFilters(BrowseFilters(genders = setOf(Gender.FEMALE)))
        assertTrue(piperAmyFemale in r)
        assertTrue(kokoroMulti in r)  // has at least one female speaker
        assertTrue(piperRyanMale !in r)
    }

    @Test
    fun `requireKnownGender hides unknown speakers`() {
        val r = catalog.applyFilters(BrowseFilters(requireKnownGender = true))
        assertTrue(unknownVoice !in r)
        assertTrue(piperAmyFemale in r)
    }

    @Test
    fun `multiSpeakerOnly hides single-speaker voices`() {
        val r = catalog.applyFilters(BrowseFilters(multiSpeakerOnly = true))
        assertEquals(listOf(kokoroMulti), r)
    }

    @Test
    fun `size bucket filter respects MB range`() {
        val r = catalog.applyFilters(BrowseFilters(sizeBuckets = setOf(SizeBucket.SMALL)))
        assertTrue(piperAmyFemale in r)         // 28 MB
        assertTrue(piperRyanMale !in r)         // 75 MB
        assertTrue(kokoroMulti !in r)           // 320 MB
    }

    @Test
    fun `loosen drops the most-recent constraint`() {
        val full = BrowseFilters(
            genders = setOf(Gender.FEMALE),
            multiSpeakerOnly = true,
            requireKnownGender = true,
        )
        // First loosen drops requireKnownGender.
        val step1 = full.loosen()
        assertEquals(false, step1.requireKnownGender)
        assertEquals(true, step1.multiSpeakerOnly)
        // Then drops multiSpeakerOnly.
        val step2 = step1.loosen()
        assertEquals(false, step2.multiSpeakerOnly)
        assertEquals(setOf(Gender.FEMALE), step2.genders)
    }
}
