package dev.ahmedmohamed.hayaitts.data.telemetry

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide ring buffer of synthesis telemetry. [SherpaSynthesisGateway]
 * and [HayaiTtsService] push [Event]s here; the Activity tab's Live
 * Generations + Requests panes subscribe.
 *
 * Two surfaces:
 *  - [events] — hot SharedFlow, fires each event exactly once.
 *  - [recent] — StateFlow of the last [RING_SIZE] events for steady-state
 *    rendering when the user navigates to the tab mid-stream.
 */
class SynthesisTelemetryRepository {

    data class Event(
        val voiceId: String,
        val sid: Int,
        val textLength: Int,
        val audioMs: Long,
        val synthMs: Long,
        val callerPackage: String?,
        val locale: String?,
        val timestamp: Long = System.currentTimeMillis(),
    ) {
        val rtf: Double get() = if (audioMs <= 0) 0.0 else synthMs.toDouble() / audioMs.toDouble()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 32)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _recent = MutableStateFlow<List<Event>>(emptyList())
    val recent: StateFlow<List<Event>> = _recent.asStateFlow()

    fun record(event: Event) {
        _events.tryEmit(event)
        _recent.update { current ->
            (listOf(event) + current).take(RING_SIZE)
        }
    }

    fun clear() {
        _recent.value = emptyList()
    }

    companion object {
        const val RING_SIZE = 100
    }
}
