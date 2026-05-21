package dev.ahmedmohamed.hayaitts.domain.model

import kotlinx.serialization.Serializable

/**
 * One speaker inside a multi-speaker voice bundle. Most Piper voices have a
 * single speaker; we still model it as a list so multi-speaker LibriTTS-R-style
 * checkpoints can be added later without a schema change.
 */
@Serializable
data class Speaker(
    val id: Int,
    val name: String,
    val gender: String,
    /**
     * Provenance of the [gender] value. Catalog entries scraped from upstream
     * model cards declare one of:
     *   - "declared" — Kokoro/Kitten naming pattern (`af_*`, `bm_*`, `*-f`, etc.)
     *     directly encodes gender; treat as ground truth.
     *   - "inferred" — Piper voice looked up through the hand-curated table in
     *     `tools/catalog/build_catalog.py`; high confidence but human-curated.
     *   - "unknown" — no signal available; voice will not appear under "Female"
     *     or "Male" Browse filters unless the user toggles "Show unknowns".
     * Defaults to "unknown" so legacy catalog JSON parses cleanly.
     */
    val genderConfidence: String = "unknown",
)
