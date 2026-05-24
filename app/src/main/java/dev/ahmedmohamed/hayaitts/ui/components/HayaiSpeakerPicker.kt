@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ahmedmohamed.hayaitts.domain.model.Speaker

/**
 * Compact dropdown picker used inside Voice cards and Voice Detail's audition
 * row. Renders as an [OutlinedButton] with the current speaker name and a
 * chevron; tapping opens a [DropdownMenu] listing every speaker.
 *
 * Hidden when there's only one speaker — callers don't need to guard.
 */
@Composable
fun HayaiSpeakerPickerInline(
    speakers: List<Speaker>,
    selectedSid: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (speakers.size <= 1) return
    val selected = speakers.firstOrNull { it.id == selectedSid } ?: speakers.first()
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { open = true }) {
            Icon(
                imageVector = iconFor(selected.gender),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(selected.name, maxLines = 1)
            Spacer(Modifier.size(4.dp))
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
        ) {
            speakers.forEach { sp ->
                DropdownMenuItem(
                    text = { Text(sp.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = iconFor(sp.gender),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        open = false
                        onPick(sp.id)
                    },
                )
            }
        }
    }
}

/**
 * Full-bleed avatars row used on Voice Detail when the voice is installed.
 * Each speaker shows as a 72dp circular avatar with a ring on selection.
 */
@Composable
fun HayaiSpeakerPickerAvatars(
    speakers: List<Speaker>,
    selectedSid: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (speakers.isEmpty()) return
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = speakers, key = { it.id }) { sp ->
            SpeakerAvatar(
                speaker = sp,
                selected = sp.id == selectedSid,
                onPick = { onPick(sp.id) },
            )
        }
    }
}

@Composable
private fun SpeakerAvatar(
    speaker: Speaker,
    selected: Boolean,
    onPick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val ringDp: Dp by animateDpAsState(
        targetValue = if (selected) 3.dp else 0.dp,
        animationSpec = spring(stiffness = 320f),
        label = "speaker-ring",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(
                    if (ringDp > 0.dp) {
                        Modifier.border(
                            width = ringDp,
                            color = accent,
                            shape = CircleShape,
                        )
                    } else Modifier,
                )
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(speaker.gender),
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = speaker.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private fun iconFor(gender: String): ImageVector = when (gender.lowercase()) {
    "f", "female" -> Icons.Outlined.Female
    "m", "male" -> Icons.Outlined.Person
    else -> Icons.Outlined.RecordVoiceOver
}
