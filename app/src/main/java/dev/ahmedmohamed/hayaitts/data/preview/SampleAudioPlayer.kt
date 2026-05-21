package dev.ahmedmohamed.hayaitts.data.preview

import android.media.AudioAttributes
import android.media.MediaPlayer
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Streams a remote audio sample (HuggingFace-hosted `.mp3` / `.wav`) over
 * HTTP and plays it inline via [MediaPlayer]. Used by Voice Detail's
 * "Listen sample" button so users can audition a voice **before** committing
 * to the multi-MB model download.
 *
 * Only one stream plays at a time. Successive [play] calls cancel the
 * previous stream so toggling between voices feels instant. The instance is
 * a singleton wired through Koin; callers don't manage lifecycle.
 */
class SampleAudioPlayer(
    private val dispatchers: DispatcherProvider,
) {
    private val log = Logger.withTag("SampleAudioPlayer")

    sealed interface State {
        data object Idle : State
        data class Loading(val url: String) : State
        data class Playing(val url: String) : State
        data class Error(val url: String, val reason: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var current: MediaPlayer? = null

    suspend fun play(url: String): Unit = withContext(dispatchers.io) {
        stop()
        _state.value = State.Loading(url)
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                setOnPreparedListener {
                    _state.value = State.Playing(url)
                    it.start()
                }
                setOnCompletionListener {
                    _state.value = State.Idle
                    it.release()
                    if (current === it) current = null
                }
                setOnErrorListener { mp, what, extra ->
                    log.w { "MediaPlayer error what=$what extra=$extra for $url" }
                    _state.value = State.Error(url, "playback failed (code $what)")
                    mp.release()
                    if (current === mp) current = null
                    true
                }
                setDataSource(url)
                prepareAsync()
            }
            current = mp
        } catch (t: Throwable) {
            _state.value = State.Error(url, t.message ?: t.javaClass.simpleName)
        }
    }

    fun stop() {
        val mp = current ?: return
        current = null
        runCatching { mp.stop() }
        runCatching { mp.release() }
        _state.value = State.Idle
    }
}
