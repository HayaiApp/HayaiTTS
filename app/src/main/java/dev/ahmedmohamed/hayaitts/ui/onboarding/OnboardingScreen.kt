@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.onboarding

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.Tier
import dev.ahmedmohamed.hayaitts.domain.recommendation.recommendedTier
import kotlinx.coroutines.launch

/**
 * First-launch onboarding flow. 4 cards in a [HorizontalPager]:
 *
 *  1. Welcome — value prop ("Offline neural TTS for Android").
 *  2. Set as engine — opens the system TTS engine picker.
 *  3. Pick voices for your device — recommended tier from [recommendedTier] +
 *     Low / Mid / High primer.
 *  4. Ready — primary CTA marks onboarding complete and routes to Library.
 *
 * Strings live under the `onboarding_*` prefix so the i18n agent can translate
 * them in a follow-up pass. Surfaces are flat — every card uses
 * `surfaceContainer` directly; no gradients, no per-card tinting.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val recommended = remember { recommendedTier(context) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                pageSpacing = 16.dp,
            ) { page ->
                when (page) {
                    0 -> WelcomeCard()
                    1 -> EngineCard(
                        onOpenSystemSettings = {
                            // android.settings.TTS_SETTINGS is routed by the
                            // framework's Settings package; wrap so a missing
                            // handler (stripped vendor ROM) cannot crash us.
                            runCatching {
                                context.startActivity(
                                    Intent("com.android.settings.TTS_SETTINGS")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                    )
                    2 -> TierCard(recommended = recommended)
                    3 -> ReadyCard(onGetStarted = onComplete)
                }
            }

            PageIndicator(
                pageCount = 4,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage < 3) {
                    TextButton(onClick = onComplete) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                    Button(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                } else {
                    Spacer(Modifier.size(1.dp))
                    Spacer(Modifier.size(1.dp))
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard() {
    OnboardingCard(
        icon = Icons.Outlined.RecordVoiceOver,
        headline = stringResource(R.string.onboarding_welcome_title),
        body = stringResource(R.string.onboarding_welcome_body),
    )
}

@Composable
private fun EngineCard(onOpenSystemSettings: () -> Unit) {
    OnboardingCard(
        icon = Icons.Outlined.Speed,
        headline = stringResource(R.string.onboarding_engine_title),
        body = stringResource(R.string.onboarding_engine_body),
        action = {
            FilledTonalButton(onClick = onOpenSystemSettings) {
                Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.onboarding_engine_action))
            }
        },
    )
}

@Composable
private fun TierCard(recommended: Tier) {
    val recommendedLabel = when (recommended) {
        Tier.LOW -> stringResource(R.string.onboarding_tier_recommendation_low)
        Tier.MID -> stringResource(R.string.onboarding_tier_recommendation_mid)
        Tier.HIGH -> stringResource(R.string.onboarding_tier_recommendation_high)
    }
    OnboardingCard(
        icon = Icons.Outlined.AutoAwesome,
        headline = stringResource(R.string.onboarding_tier_title),
        body = recommendedLabel,
        action = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TierRow(stringResource(R.string.onboarding_tier_low))
                TierRow(stringResource(R.string.onboarding_tier_mid))
                TierRow(stringResource(R.string.onboarding_tier_high))
            }
        },
    )
}

@Composable
private fun ReadyCard(onGetStarted: () -> Unit) {
    OnboardingCard(
        icon = Icons.Outlined.LibraryMusic,
        headline = stringResource(R.string.onboarding_ready_title),
        body = stringResource(R.string.onboarding_ready_body),
        action = {
            Button(onClick = onGetStarted) {
                Text(stringResource(R.string.onboarding_ready_action))
            }
        },
    )
}

@Composable
private fun TierRow(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun OnboardingCard(
    icon: ImageVector,
    headline: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (action != null) {
                Spacer(Modifier.height(4.dp))
                action()
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            val active = i == currentPage
            val color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (active) 24.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
