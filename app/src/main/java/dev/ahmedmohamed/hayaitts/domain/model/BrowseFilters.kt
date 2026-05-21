package dev.ahmedmohamed.hayaitts.domain.model

/**
 * v2 filter model for Browse. Replaces the loose `Set<String>` per dimension
 * the v1 screen carried. Each dimension is a `Set<...>` — empty means "don't
 * filter on this dimension". The screen toggles chips; the VM stores the
 * snapshot and recomputes the filtered list.
 */
data class BrowseFilters(
    val genders: Set<Gender> = emptySet(),
    val languages: Set<String> = emptySet(),
    val families: Set<ModelFamily> = emptySet(),
    val tiers: Set<Tier> = emptySet(),
    val licenses: Set<String> = emptySet(),
    val sampleRateBuckets: Set<SampleRateBucket> = emptySet(),
    val sizeBuckets: Set<SizeBucket> = emptySet(),
    val multiSpeakerOnly: Boolean = false,
    val availableOnly: Boolean = false,
    val requireKnownGender: Boolean = false,
) {
    val isEmpty: Boolean get() =
        genders.isEmpty() && languages.isEmpty() && families.isEmpty() &&
            tiers.isEmpty() && licenses.isEmpty() && sampleRateBuckets.isEmpty() &&
            sizeBuckets.isEmpty() && !multiSpeakerOnly && !availableOnly && !requireKnownGender

    /** Drop the most-recently-added constraint. Used by the "Loosen filter" CTA. */
    fun loosen(): BrowseFilters = when {
        requireKnownGender -> copy(requireKnownGender = false)
        availableOnly -> copy(availableOnly = false)
        multiSpeakerOnly -> copy(multiSpeakerOnly = false)
        sizeBuckets.isNotEmpty() -> copy(sizeBuckets = sizeBuckets.drop(1).toSet())
        sampleRateBuckets.isNotEmpty() -> copy(sampleRateBuckets = sampleRateBuckets.drop(1).toSet())
        licenses.isNotEmpty() -> copy(licenses = licenses.drop(1).toSet())
        tiers.isNotEmpty() -> copy(tiers = tiers.drop(1).toSet())
        families.isNotEmpty() -> copy(families = families.drop(1).toSet())
        languages.isNotEmpty() -> copy(languages = languages.drop(1).toSet())
        genders.isNotEmpty() -> copy(genders = genders.drop(1).toSet())
        else -> this
    }
}

enum class SampleRateBucket(val hz: IntRange) {
    KHZ_16(15_000..18_000),
    KHZ_22(20_000..23_000),
    KHZ_24(23_001..25_000),
    KHZ_48(40_000..50_000);

    fun matches(hz: Int): Boolean = hz in this.hz
}

enum class SizeBucket(val mb: IntRange) {
    SMALL(0..30),
    MEDIUM(31..80),
    LARGE(81..Int.MAX_VALUE);

    fun matches(mb: Int): Boolean = mb in this.mb
}

/** Applies [filters] to a list of voice cards. Used by Browse and any "did this query land?" preview. */
fun List<VoiceCard>.applyFilters(filters: BrowseFilters): List<VoiceCard> {
    if (filters.isEmpty) return this
    return filter { card ->
        // Gender — match if ANY speaker's gender is in the selected set.
        if (filters.genders.isNotEmpty()) {
            val ok = card.speakers.any { sp ->
                Gender.parse(sp.gender) in filters.genders
            }
            if (!ok) return@filter false
        }
        if (filters.requireKnownGender) {
            val anyKnown = card.speakers.any { sp ->
                val g = Gender.parse(sp.gender)
                g == Gender.FEMALE || g == Gender.MALE || g == Gender.NEUTRAL
            }
            if (!anyKnown) return@filter false
        }
        if (filters.languages.isNotEmpty()) {
            if (card.languages.none { it in filters.languages }) return@filter false
        }
        if (filters.families.isNotEmpty()) {
            val fam = ModelFamily.fromCatalog(card.family)
            if (fam !in filters.families) return@filter false
        }
        if (filters.tiers.isNotEmpty()) {
            val tier = Tier.fromCatalog(card.tier)
            if (tier !in filters.tiers) return@filter false
        }
        if (filters.licenses.isNotEmpty()) {
            if (card.license !in filters.licenses) return@filter false
        }
        if (filters.sampleRateBuckets.isNotEmpty()) {
            if (filters.sampleRateBuckets.none { it.matches(card.sampleRateHz) }) return@filter false
        }
        if (filters.sizeBuckets.isNotEmpty()) {
            if (filters.sizeBuckets.none { it.matches(card.approxSizeMb) }) return@filter false
        }
        if (filters.multiSpeakerOnly && card.speakers.size <= 1) return@filter false
        if (filters.availableOnly && !card.available) return@filter false
        true
    }
}
