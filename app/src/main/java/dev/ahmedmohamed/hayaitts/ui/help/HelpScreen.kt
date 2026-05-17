@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.ui.theme.HayaiTtsTheme

/**
 * In-app help / FAQ screen reached from Settings → About → "Help". Scrollable
 * list of [ExpandableCard]s; each card collapses by default and expands with
 * an [AnimatedVisibility] reveal. Flat M3 — no gradients.
 */
class HelpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HayaiTtsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HelpScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                subtitle = { Text(stringResource(R.string.help_subtitle)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("getting_started") {
                ExpandableCard(
                    icon = Icons.Outlined.Help,
                    title = stringResource(R.string.help_section_getting_started_title),
                    body = stringResource(R.string.help_section_getting_started_body),
                )
            }
            item("choose_voice") {
                ExpandableCard(
                    icon = Icons.Outlined.Tune,
                    title = stringResource(R.string.help_section_choose_voice_title),
                    body = stringResource(R.string.help_section_choose_voice_body),
                )
            }
            item("locale_defaults") {
                ExpandableCard(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.help_section_locale_defaults_title),
                    body = stringResource(R.string.help_section_locale_defaults_body),
                )
            }
            item("custom_voices") {
                ExpandableCard(
                    icon = Icons.Outlined.Mic,
                    title = stringResource(R.string.help_section_custom_voices_title),
                    body = stringResource(R.string.help_section_custom_voices_body),
                    action = {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(SHERPA_DOCS_URL))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.help_section_custom_voices_action))
                        }
                    },
                )
            }
            item("storage") {
                ExpandableCard(
                    icon = Icons.Outlined.SdStorage,
                    title = stringResource(R.string.help_section_storage_title),
                    body = stringResource(R.string.help_section_storage_body),
                )
            }
            item("updates") {
                ExpandableCard(
                    icon = Icons.Outlined.CloudDownload,
                    title = stringResource(R.string.help_section_updates_title),
                    body = stringResource(R.string.help_section_updates_body),
                )
            }
            item("troubleshooting") {
                ExpandableCard(
                    icon = Icons.Outlined.BugReport,
                    title = stringResource(R.string.help_section_troubleshooting_title),
                    body = stringResource(R.string.help_section_troubleshooting_body),
                )
            }
            item("about") {
                ExpandableCard(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.help_section_about_title),
                    body = stringResource(R.string.help_section_about_body),
                    action = {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.help_section_about_action))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ExpandableCard(
    icon: ImageVector,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron-rot")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (action != null) action()
                }
            }
        }
    }
}

private const val REPO_URL = "https://github.com/avnology/HayaiTTS"
private const val SHERPA_DOCS_URL =
    "https://k2-fsa.github.io/sherpa/onnx/tts/index.html"
