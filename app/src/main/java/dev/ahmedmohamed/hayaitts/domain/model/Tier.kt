package dev.ahmedmohamed.hayaitts.domain.model

/**
 * Voice quality / size tier as advertised in the catalog. The values match the
 * `tier` field in catalog/v1/models.json so they can be parsed via [valueOf]
 * after upper-casing.
 */
enum class Tier {
    LOW,
    MID,
    HIGH;

    companion object {
        fun fromCatalog(raw: String): Tier = when (raw.lowercase()) {
            "low" -> LOW
            "mid", "medium" -> MID
            "high" -> HIGH
            else -> MID
        }
    }
}
