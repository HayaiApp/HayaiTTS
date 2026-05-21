@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.onboarding

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.Tier
import dev.ahmedmohamed.hayaitts.domain.recommendation.recommendedTier
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/**
 * First-launch onboarding flow. 4 expressive pages in a [HorizontalPager]:
 *
 *  1. Welcome — value prop.
 *  2. Set as engine — opens the system TTS engine picker.
 *  3. Pick voices for your device — recommended tier from [recommendedTier].
 *  4. Ready — primary CTA marks onboarding complete and routes to Library.
 *
 * Each page owns a distinct container color (primary / tertiary / secondary
 * containers, rotating) plus a hero shape whose corner radii morph between an
 * asymmetric cookie and a softer round on every page change. The page
 * indicator and pager animate together; off-screen pages parallax slightly
 * via `graphicsLayer`.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val recommended = remember { recommendedTier(context) }

    val accent by animateColorAsState(
        targetValue = accentForPage(pagerState.currentPage),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "onboarding-accent",
    )
    val onAccent by animateColorAsState(
        targetValue = onAccentForPage(pagerState.currentPage),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "onboarding-on-accent",
    )

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
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                pageSpacing = 16.dp,
            ) { page ->
                val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue
                val parallax = (1f - pageOffset.coerceIn(0f, 1f) * 0.06f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = parallax
                            scaleY = parallax
                            alpha = 1f - pageOffset.coerceIn(0f, 1f) * 0.4f
                        },
                ) {
                    when (page) {
                        0 -> WelcomeCard(accent = accent, onAccent = onAccent, page = page)
                        1 -> EngineCard(
                            accent = accent,
                            onAccent = onAccent,
                            page = page,
                            onOpenSystemSettings = {
                                runCatching {
                                    context.startActivity(
                                        Intent("com.android.settings.TTS_SETTINGS")
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            },
                        )
                        2 -> TierCard(
                            recommended = recommended,
                            accent = accent,
                            onAccent = onAccent,
                            page = page,
                        )
                        3 -> ReadyCard(
                            accent = accent,
                            onAccent = onAccent,
                            page = page,
                            onGetStarted = onComplete,
                        )
                    }
                }
            }

            PageIndicator(
                pageCount = 4,
                currentPage = pagerState.currentPage,
                activeColor = accent,
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
                    Button(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = onAccent,
                        ),
                    ) {
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
private fun WelcomeCard(accent: Color, onAccent: Color, page: Int) {
    OnboardingCard(
        accent = accent,
        onAccent = onAccent,
        page = page,
        icon = Icons.Outlined.RecordVoiceOver,
        headline = stringResource(R.string.onboarding_welcome_title),
        body = stringResource(R.string.onboarding_welcome_body),
    )
}

@Composable
private fun EngineCard(
    accent: Color,
    onAccent: Color,
    page: Int,
    onOpenSystemSettings: () -> Unit,
) {
    OnboardingCard(
        accent = accent,
        onAccent = onAccent,
        page = page,
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
private fun TierCard(recommended: Tier, accent: Color, onAccent: Color, page: Int) {
    val recommendedLabel = when (recommended) {
        Tier.LOW -> stringResource(R.string.onboarding_tier_recommendation_low)
        Tier.MID -> stringResource(R.string.onboarding_tier_recommendation_mid)
        Tier.HIGH -> stringResource(R.string.onboarding_tier_recommendation_high)
    }
    OnboardingCard(
        accent = accent,
        onAccent = onAccent,
        page = page,
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
private fun ReadyCard(accent: Color, onAccent: Color, page: Int, onGetStarted: () -> Unit) {
    OnboardingCard(
        accent = accent,
        onAccent = onAccent,
        page = page,
        icon = Icons.Outlined.LibraryMusic,
        headline = stringResource(R.string.onboarding_ready_title),
        body = stringResource(R.string.onboarding_ready_body),
        action = {
            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = onAccent,
                ),
            ) {
                Text(stringResource(R.string.onboarding_ready_action))
            }
        },
    )
}

@Composable
private fun TierRow(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun OnboardingCard(
    accent: Color,
    onAccent: Color,
    page: Int,
    icon: ImageVector,
    headline: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    val cardShape = expressiveShapeForPage(page)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = accent,
        shape = cardShape,
        contentColor = onAccent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            HeroShape(
                icon = icon,
                page = page,
                accent = accent,
                onAccent = onAccent,
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
                color = onAccent,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = onAccent.copy(alpha = 0.85f),
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

/**
 * Hero badge: each page gets a distinct asymmetric corner profile so the
 * shape contributes to the visual identity of the page. The HorizontalPager
 * handles the cross-fade between adjacent shapes as the user swipes.
 */
@Composable
private fun HeroShape(
    icon: ImageVector,
    page: Int,
    accent: Color,
    onAccent: Color,
) {
    val heroShape = when (page % 4) {
        0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 56.dp, bottomEnd = 32.dp, bottomStart = 56.dp)
        1 -> RoundedCornerShape(topStart = 56.dp, topEnd = 28.dp, bottomEnd = 56.dp, bottomStart = 32.dp)
        2 -> RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomEnd = 64.dp, bottomStart = 16.dp)
        else -> RoundedCornerShape(topStart = 64.dp, topEnd = 16.dp, bottomEnd = 40.dp, bottomStart = 40.dp)
    }
    Surface(
        shape = heroShape,
        color = onAccent.copy(alpha = 0.18f),
        modifier = Modifier.size(132.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onAccent,
                modifier = Modifier.size(60.dp),
            )
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            val active = i == currentPage
            val color by animateColorAsState(
                targetValue = if (active) activeColor else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = spring(),
                label = "indicator-color-$i",
            )
            val width by animateDpAsState(
                targetValue = if (active) 32.dp else 8.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "indicator-width-$i",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun accentForPage(page: Int): Color = when (page % 4) {
    0 -> MaterialTheme.colorScheme.primaryContainer
    1 -> MaterialTheme.colorScheme.tertiaryContainer
    2 -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun onAccentForPage(page: Int): Color = when (page % 4) {
    0 -> MaterialTheme.colorScheme.onPrimaryContainer
    1 -> MaterialTheme.colorScheme.onTertiaryContainer
    2 -> MaterialTheme.colorScheme.onSecondaryContainer
    else -> MaterialTheme.colorScheme.onPrimary
}

private fun expressiveShapeForPage(page: Int): RoundedCornerShape {
    val offset = (page * 6) % 16
    return RoundedCornerShape(
        topStart = (32 + offset).dp,
        topEnd = (48 - offset).dp,
        bottomEnd = (32 + offset).dp,
        bottomStart = (48 - offset).dp,
    )
}
