package dev.ahmedmohamed.hayaitts.domain.model

/**
 * Progress states emitted by `StorageMigrator.moveAllVoices`. The Settings
 * screen toggles a wavy progress indicator + disables the storage-location
 * radio while [Moving], surfaces the final count on [Done], or renders the
 * reason on [Failed].
 */
sealed interface MoveProgress {
    data object Idle : MoveProgress

    /**
     * @param currentVoiceId the voice currently being copied, or null between
     *   voices (e.g. while updating the Room row).
     * @param doneCount how many voices are already at the new location.
     * @param totalCount how many voices need to move in total.
     */
    data class Moving(
        val currentVoiceId: String?,
        val doneCount: Int,
        val totalCount: Int,
    ) : MoveProgress {
        val fraction: Float
            get() = if (totalCount == 0) 1f else doneCount.toFloat() / totalCount
    }

    data class Done(val movedCount: Int) : MoveProgress
    data class Failed(val reason: String, val partialCount: Int) : MoveProgress
}
