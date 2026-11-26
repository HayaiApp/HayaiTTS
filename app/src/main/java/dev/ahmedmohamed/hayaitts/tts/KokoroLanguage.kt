package dev.ahmedmohamed.hayaitts.tts

import java.io.File

/**
 * Maps a language to the `lang` value multi-lang Kokoro (>= v1.0) expects.
 *
 * sherpa-onnx hands `lang` straight to eSpeak-ng's `SetVoiceByName` for every
 * non-Chinese chunk of text, and an unknown voice name aborts the process
 * (`Failed to set eSpeak-ng voice`, an uncaught C++ exception, so Kotlin can't
 * catch it). That's why `你好Hello` or `78` crashed with `lang=zh`: there is no
 * eSpeak voice file called `zh`.
 *
 * - zh / en: return "" so sherpa uses the bundled lexicons (`lexicon-zh.txt`,
 *   `lexicon-us-en.txt`), which is also the higher-quality path for English.
 * - others: the eSpeak voice *file* name, looked up case-insensitively in
 *   `espeak-ng-data/lang`, so we only ever pass a name that exists.
 */
object KokoroLanguage {

    private val ESPEAK_NAMES = mapOf(
        "es" to listOf("es"),
        "fr" to listOf("fr", "fr-FR"),
        "hi" to listOf("hi"),
        "it" to listOf("it"),
        "pt" to listOf("pt-BR", "pt"),
        "ja" to listOf("ja"),
    )

    fun espeakVoiceFor(language: String, espeakDataDir: File?): String {
        val head = language.trim().replace('_', '-').substringBefore('-').lowercase()
        val wanted = ESPEAK_NAMES[head] ?: return ""
        val available = espeakVoiceFiles(espeakDataDir)
        for (name in wanted) {
            available.firstOrNull { it.equals(name, ignoreCase = true) }?.let { return it }
        }
        return ""
    }

    private fun espeakVoiceFiles(dataDir: File?): List<String> {
        val langDir = dataDir?.let { File(it, "lang") }?.takeIf { it.isDirectory } ?: return emptyList()
        return langDir.walkTopDown().filter { it.isFile }.map { it.name }.toList()
    }
}
