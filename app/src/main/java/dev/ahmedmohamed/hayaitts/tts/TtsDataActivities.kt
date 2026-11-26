package dev.ahmedmohamed.hayaitts.tts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech

/**
 * `ACTION_CHECK_TTS_DATA` handler. System Settings (and some apps) start this
 * before enabling the engine's sample/Play button; without it Settings logs
 * "Failed to check TTS data, no activity found" and stays silent.
 *
 * Reports the locales of installed voices in the ISO3 `lang-COUNTRY` form the
 * framework expects. No installed voices → FAIL, so callers can send the user
 * to install one.
 */
class CheckVoiceDataActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread {
            val tags = runCatching { SherpaTtsRuntime.get(applicationContext).listAvailableVoices() }
                .getOrDefault(emptyList())
                .flatMap { it.languages }
                .mapNotNull { TtsLocales.toIso3(TtsLocales.toBcp47(it)) }
                .distinct()
            val data = Intent()
                .putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, ArrayList(tags))
                .putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, ArrayList())
            runOnUiThread {
                setResult(
                    if (tags.isEmpty()) TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL else TextToSpeech.Engine.CHECK_VOICE_DATA_PASS,
                    data,
                )
                finish()
            }
        }.start()
    }
}

/** `ACTION_GET_SAMPLE_TEXT` handler: a short sentence in the requested language. */
class GetSampleTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lang = TtsLocales.toBcp47(intent?.getStringExtra("language")).substringBefore('-')
        val text = SAMPLES[lang] ?: SAMPLES.getValue("en")
        setResult(TextToSpeech.LANG_AVAILABLE, Intent().putExtra("sampleText", text))
        finish()
    }

    private companion object {
        val SAMPLES = mapOf(
            "en" to "This is an example of speech synthesis with HayaiTTS.",
            "de" to "Dies ist ein Beispiel für Sprachsynthese mit HayaiTTS.",
            "fr" to "Ceci est un exemple de synthèse vocale avec HayaiTTS.",
            "es" to "Este es un ejemplo de síntesis de voz con HayaiTTS.",
            "it" to "Questo è un esempio di sintesi vocale con HayaiTTS.",
            "pt" to "Este é um exemplo de síntese de voz com o HayaiTTS.",
            "nl" to "Dit is een voorbeeld van spraaksynthese met HayaiTTS.",
            "ru" to "Это пример синтеза речи с помощью HayaiTTS.",
            "zh" to "这是使用 HayaiTTS 进行语音合成的示例。",
            "ja" to "これは HayaiTTS による音声合成の例です。",
            "ko" to "HayaiTTS를 사용한 음성 합성의 예입니다.",
            "ar" to "هذا مثال على تحويل النص إلى كلام باستخدام HayaiTTS.",
        )
    }
}
