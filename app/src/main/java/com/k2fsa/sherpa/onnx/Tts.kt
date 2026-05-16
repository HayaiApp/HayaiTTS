// Hand-port of the upstream Kotlin TTS bindings for sherpa-onnx 6.25.12.
//
// The Maven Central re-package we depend on (com.bihe0832.android:lib-sherpa-onnx:6.25.12)
// ships the JNI .so with TTS support — every `Java_com_k2fsa_sherpa_onnx_OfflineTts_*`
// symbol is present in libsherpa-onnx-jni.so — but it omits the Kotlin/Java glue
// for the TTS classes (it only includes the ASR bindings). To use those native
// symbols we have to redeclare matching `external fun` stubs in the
// `com.k2fsa.sherpa.onnx` package so the JVM linker finds them.
//
// The class shape, field names, and constructor signatures must match what the
// 6.25.12 .so reads via JNI reflection — extra fields are tolerated, missing
// fields throw NoSuchFieldError at first use. The native side in 6.25.12 looks
// up the following:
//   OfflineTtsConfig:        model, ruleFsts, ruleFars, maxNumSentences
//   OfflineTtsModelConfig:   vits, matcha, numThreads, debug, provider
//   OfflineTtsVitsModelConfig: model, lexicon, tokens, dataDir, dictDir,
//                              noiseScale, noiseScaleW, lengthScale
//   OfflineTtsMatchaModelConfig: acousticModel, vocoder, lexicon, tokens,
//                                dataDir, dictDir, noiseScale, lengthScale
//   GeneratedAudio:          (FloatArray samples, int sampleRate) constructor
//
// Newer sherpa-onnx releases add Kokoro / Kitten / ZipVoice / Pocket / Supertonic
// model configs plus a richer GenerationConfig — those are intentionally NOT
// declared here because the 6.25.12 native side does not know how to read them.

// Copyright (c) 2023 Xiaomi Corporation — original upstream definitions are
// MIT-licensed (see https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE).
package com.k2fsa.sherpa.onnx

import android.content.res.AssetManager

data class OfflineTtsVitsModelConfig(
    var model: String = "",
    var lexicon: String = "",
    var tokens: String = "",
    var dataDir: String = "",
    var dictDir: String = "",
    var noiseScale: Float = 0.667f,
    var noiseScaleW: Float = 0.8f,
    var lengthScale: Float = 1.0f,
)

data class OfflineTtsMatchaModelConfig(
    var acousticModel: String = "",
    var vocoder: String = "",
    var lexicon: String = "",
    var tokens: String = "",
    var dataDir: String = "",
    var dictDir: String = "",
    var noiseScale: Float = 1.0f,
    var lengthScale: Float = 1.0f,
)

data class OfflineTtsModelConfig(
    var vits: OfflineTtsVitsModelConfig = OfflineTtsVitsModelConfig(),
    var matcha: OfflineTtsMatchaModelConfig = OfflineTtsMatchaModelConfig(),
    var numThreads: Int = 1,
    var debug: Boolean = false,
    var provider: String = "cpu",
)

data class OfflineTtsConfig(
    var model: OfflineTtsModelConfig = OfflineTtsModelConfig(),
    var ruleFsts: String = "",
    var ruleFars: String = "",
    var maxNumSentences: Int = 1,
)

class GeneratedAudio(
    val samples: FloatArray,
    val sampleRate: Int,
) {
    fun save(filename: String) =
        saveImpl(filename = filename, samples = samples, sampleRate = sampleRate)

    private external fun saveImpl(
        filename: String,
        samples: FloatArray,
        sampleRate: Int,
    ): Boolean
}

class OfflineTts(
    assetManager: AssetManager? = null,
    var config: OfflineTtsConfig,
) {
    private var ptr: Long

    init {
        ptr = if (assetManager != null) {
            newFromAsset(assetManager, config)
        } else {
            newFromFile(config)
        }
    }

    fun sampleRate(): Int = getSampleRate(ptr)
    fun numSpeakers(): Int = getNumSpeakers(ptr)

    fun generate(
        text: String,
        sid: Int = 0,
        speed: Float = 1.0f,
    ): GeneratedAudio = generateImpl(ptr, text = text, sid = sid, speed = speed)

    fun generateWithCallback(
        text: String,
        sid: Int = 0,
        speed: Float = 1.0f,
        callback: (samples: FloatArray) -> Int,
    ): GeneratedAudio = generateWithCallbackImpl(
        ptr, text = text, sid = sid, speed = speed, callback = callback,
    )

    @Suppress("ProtectedInFinal")
    protected fun finalize() {
        if (ptr != 0L) {
            delete(ptr)
            ptr = 0L
        }
    }

    fun release() = finalize()

    private external fun newFromAsset(
        assetManager: AssetManager,
        config: OfflineTtsConfig,
    ): Long

    private external fun newFromFile(config: OfflineTtsConfig): Long
    private external fun delete(ptr: Long)
    private external fun getSampleRate(ptr: Long): Int
    private external fun getNumSpeakers(ptr: Long): Int

    private external fun generateImpl(
        ptr: Long,
        text: String,
        sid: Int = 0,
        speed: Float = 1.0f,
    ): GeneratedAudio

    private external fun generateWithCallbackImpl(
        ptr: Long,
        text: String,
        sid: Int = 0,
        speed: Float = 1.0f,
        callback: (samples: FloatArray) -> Int,
    ): GeneratedAudio

    companion object {
        init {
            System.loadLibrary("sherpa-onnx-jni")
        }
    }
}
