package dev.ahmedmohamed.hayaitts.tts

import java.util.Locale
import java.util.MissingResourceException

/**
 * Locale plumbing between Android's TTS framework and the catalog.
 *
 * The framework talks ISO 639-2/3166 alpha-3 (`eng`, `USA`, see
 * [android.speech.tts.TextToSpeechService.onIsLanguageAvailable]); the catalog
 * and Room store BCP-47 (`en-US`). Comparing the two directly never matched,
 * so `isLanguageAvailable("eng")` said "not supported" and clients like the
 * Settings sample or browser read-aloud went silent.
 */
object TtsLocales {

    const val NOT_SUPPORTED = 0
    const val LANGUAGE = 1
    const val COUNTRY = 2

    private val iso3ToLanguage: Map<String, String> by lazy {
        Locale.getISOLanguages().mapNotNull { l ->
            runCatching { Locale(l).isO3Language }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { it.lowercase() to l }
        }.toMap()
    }

    private val iso3ToCountry: Map<String, String> by lazy {
        Locale.getISOCountries().mapNotNull { c ->
            runCatching { Locale("", c).isO3Country }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { it.uppercase() to c }
        }.toMap()
    }

    /** `("eng","USA")` / `("en","US")` / `("en-US", null)` → `en-US`. Blank when unusable. */
    fun toBcp47(lang: String?, country: String? = null): String {
        if (lang.isNullOrBlank()) return ""
        val tag = Locale.forLanguageTag(lang.replace('_', '-'))
        val rawLang = tag.language.ifEmpty { lang }.lowercase()
        val language = if (rawLang.length == 3) iso3ToLanguage[rawLang] ?: rawLang else rawLang
        val rawCountry = country?.takeIf { it.isNotBlank() } ?: tag.country
        val region = when {
            rawCountry.isNullOrBlank() -> ""
            rawCountry.length == 3 -> iso3ToCountry[rawCountry.uppercase()].orEmpty()
            else -> rawCountry.uppercase()
        }
        return if (region.isEmpty()) language else "$language-$region"
    }

    /** ISO3 `lang-COUNTRY` form used by `EXTRA_AVAILABLE_VOICES`. */
    fun toIso3(bcp47: String): String? {
        val l = Locale.forLanguageTag(bcp47)
        val lang = try { l.isO3Language } catch (_: MissingResourceException) { return null }
        if (lang.isEmpty()) return null
        val country = try { l.isO3Country } catch (_: MissingResourceException) { "" }
        return if (country.isEmpty()) lang else "$lang-$country"
    }

    /** How well a voice's BCP-47 [tags] cover [requested] (BCP-47). */
    fun matchLevel(tags: List<String>, requested: String): Int {
        val want = Locale.forLanguageTag(requested)
        if (want.language.isEmpty()) return NOT_SUPPORTED
        var best = NOT_SUPPORTED
        for (t in tags) {
            val have = Locale.forLanguageTag(toBcp47(t))
            if (!have.language.equals(want.language, ignoreCase = true)) continue
            best = maxOf(best, if (want.country.isNotEmpty() && have.country.equals(want.country, ignoreCase = true)) COUNTRY else LANGUAGE)
        }
        return best
    }
}
