package dev.ahmedmohamed.hayaitts.domain.model

/**
 * Per-speaker gender. The raw `gender` string on [Speaker] is preserved for
 * catalog stability — this enum is the structured type the filter system
 * uses.
 *
 * The catalog scraper at `tools/catalog/build_catalog.py` emits one of:
 *   - "F" → [FEMALE]
 *   - "M" → [MALE]
 *   - "N" → [NEUTRAL] (non-binary / synthetic)
 *   - anything else → [UNKNOWN]
 */
enum class Gender {
    FEMALE, MALE, NEUTRAL, UNKNOWN;

    companion object {
        fun parse(raw: String?): Gender = when (raw?.uppercase()) {
            "F", "FEMALE" -> FEMALE
            "M", "MALE" -> MALE
            "N", "NB", "NEUTRAL", "NONBINARY", "NON-BINARY" -> NEUTRAL
            else -> UNKNOWN
        }
    }
}

val Speaker.genderEnum: Gender get() = Gender.parse(gender)
