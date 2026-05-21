package dev.ahmedmohamed.hayaitts.domain.usecase

import dev.ahmedmohamed.hayaitts.domain.model.Tier

/**
 * Wraps the host-side tier estimator behind a domain-pure interface. The
 * actual estimator lives in `data/device/` because it touches Android SDK
 * types; this use case takes the pre-resolved tier from the data layer so
 * domain consumers stay Android-free.
 *
 * Browse / Voice Detail / Onboarding all call this to bubble the
 * recommendation into UI state.
 */
class RecommendTierUseCase {
    /** Pure pass-through today; reserved for future heuristics (e.g., user override). */
    operator fun invoke(deviceTier: Tier): Tier = deviceTier
}
