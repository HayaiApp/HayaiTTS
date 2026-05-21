package dev.ahmedmohamed.hayaitts.ui.custom

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import dev.ahmedmohamed.hayaitts.data.custom.CustomBundleAnalyzer
import dev.ahmedmohamed.hayaitts.data.custom.CustomBundleInstaller
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder

/**
 * Drives the multi-step custom voice import wizard. The screen subscribes to
 * [state] and renders one of [Phase.Analyzing], [Phase.Confirm],
 * [Phase.Importing], [Phase.Done], or [Phase.Failed] accordingly.
 *
 * The picked SAF [Uri] is passed in URL-encoded via a navigation argument so
 * we never need to persist arbitrary content URIs into a Room column or pass a
 * Parcelable through the back-stack.
 */
class CustomImportViewModel(
    encodedUri: String,
    private val analyzer: CustomBundleAnalyzer,
    private val installer: CustomBundleInstaller,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val uri: Uri = URLDecoder.decode(encodedUri, "UTF-8").toUri()

    enum class FamilyChoice(val family: ModelFamily, val supported: Boolean) {
        PIPER(ModelFamily.PIPER, true),
        VITS(ModelFamily.VITS, true),
        MATCHA(ModelFamily.MATCHA, true),
        KOKORO(ModelFamily.KOKORO, true),
        KITTEN(ModelFamily.KITTEN, true),
        ZIPVOICE(ModelFamily.ZIPVOICE, true),
        POCKET(ModelFamily.POCKET, true),
        SUPERTONIC(ModelFamily.SUPERTONIC, true),
    }

    sealed class Phase {
        data object Analyzing : Phase()
        data class Confirm(
            val analysis: CustomBundleAnalyzer.Analysis,
            val voiceName: String,
            val chosenFamily: ModelFamily?,
            val languages: List<String>,
            val validationError: String? = null,
        ) : Phase()
        data class Importing(val progress: CustomBundleInstaller.Progress) : Phase()
        data class Done(val voiceId: String) : Phase()
        data class Failed(val reason: String) : Phase()
    }

    private val _state = MutableStateFlow<Phase>(Phase.Analyzing)
    val state: StateFlow<Phase> = _state.asStateFlow()

    private val progressFlow = MutableStateFlow(
        CustomBundleInstaller.Progress(CustomBundleInstaller.Step.Copying, 0f),
    )

    init {
        viewModelScope.launch {
            val result = withContext(dispatchers.io) { analyzer.analyze(uri) }
            result
                .onSuccess { analysis ->
                    analysis.unsupportedFamily?.let { unsupported ->
                        _state.value = Phase.Failed(
                            "${unsupported.name.lowercase().replaceFirstChar { it.uppercase() }} bundles are not yet supported by this app build.",
                        )
                        return@onSuccess
                    }
                    _state.value = Phase.Confirm(
                        analysis = analysis,
                        voiceName = analysis.suggestedName,
                        chosenFamily = analysis.detectedFamily,
                        languages = analysis.detectedLanguages,
                    )
                }
                .onFailure { _state.value = Phase.Failed(it.message ?: "Unknown error") }
        }
        viewModelScope.launch {
            // Mirror installer progress into the importing phase so the screen
            // observes a single StateFlow.
            progressFlow.collect { p ->
                val cur = _state.value
                if (cur is Phase.Importing) _state.value = Phase.Importing(p)
            }
        }
    }

    fun updateName(name: String) {
        val cur = _state.value as? Phase.Confirm ?: return
        _state.value = cur.copy(voiceName = name)
    }

    fun updateFamily(choice: FamilyChoice) {
        val cur = _state.value as? Phase.Confirm ?: return
        if (!choice.supported) {
            _state.value = cur.copy(
                chosenFamily = null,
                validationError = "${choice.family.name.lowercase().replaceFirstChar { it.uppercase() }} is not supported on the current sherpa-onnx version.",
            )
            return
        }
        _state.value = cur.copy(chosenFamily = choice.family, validationError = null)
    }

    fun addLanguage(tag: String) {
        val cleaned = tag.trim()
        if (cleaned.isEmpty()) return
        val cur = _state.value as? Phase.Confirm ?: return
        if (cleaned in cur.languages) return
        _state.value = cur.copy(languages = cur.languages + cleaned)
    }

    fun removeLanguage(tag: String) {
        val cur = _state.value as? Phase.Confirm ?: return
        _state.value = cur.copy(languages = cur.languages - tag)
    }

    fun startImport() {
        val cur = _state.value as? Phase.Confirm ?: return
        val family = cur.chosenFamily ?: return
        val analysis = cur.analysis
        _state.value = Phase.Importing(progressFlow.value)
        viewModelScope.launch {
            val request = CustomBundleInstaller.Request(
                sourceUri = uri,
                archive = analysis.archive,
                effectiveFamily = family,
                voiceName = cur.voiceName,
                languages = cur.languages,
                speakers = analysis.speakers,
            )
            val result = withContext(dispatchers.io) {
                installer.install(request, progressFlow)
            }
            result
                .onSuccess { installed -> _state.value = Phase.Done(installed.voiceId) }
                .onFailure {
                    val msg = it.message.orEmpty()
                    if (msg.startsWith("missing:")) {
                        val missing = msg.removePrefix("missing:")
                        // Drop back into Confirm with an inline error so the
                        // user can pick another family without re-picking the
                        // file. Keep their typed-in name + languages.
                        _state.value = cur.copy(
                            validationError = "Bundle is missing $missing for ${family.name.lowercase()}.",
                        )
                    } else {
                        _state.value = Phase.Failed(msg.ifBlank { "Import failed" })
                    }
                }
        }
    }
}
