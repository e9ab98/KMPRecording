package com.e9ab98.kmprecording.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.e9ab98.kmprecording.domain.RecordingMode
import com.e9ab98.kmprecording.domain.AppLanguage
import com.e9ab98.kmprecording.domain.L10n
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality
import com.e9ab98.kmprecording.presentation.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val l10n = L10n.get(state.appLanguage)

    Row(
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text(l10n.cancelButton)
                    }
                    Text(l10n.settingsTitle, style = MaterialTheme.typography.titleMedium)
                    Text(l10n.settingsButton, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResolutionSection(
                    selected = state.resolution,
                    onSelected = viewModel::resolutionChanged,
                    l10n = l10n
                )
                QualitySection(
                    selected = state.quality,
                    onSelected = viewModel::qualityChanged,
                    l10n = l10n
                )
                ToggleSection(
                    title = l10n.audioRecording,
                    description = if (state.audioEnabled) l10n.audioRecordingEnabled else l10n.audioRecordingDisabled,
                    checked = state.audioEnabled,
                    onCheckedChange = viewModel::audioEnabledChanged
                )
                SegmentDurationSection(
                    seconds = state.segmentDurationSeconds,
                    onChange = viewModel::segmentDurationChanged,
                    l10n = l10n
                )
                MaxStorageSection(
                    gb = state.maxStorageGB,
                    onChange = viewModel::maxStorageGBChanged,
                    l10n = l10n
                )
                ToggleSection(
                    title = l10n.loopRecordingMode,
                    description = if (state.defaultMode == RecordingMode.LOOP) {
                        l10n.loopRecordingEnabledDesc
                    } else {
                        l10n.loopRecordingDisabledDesc
                    },
                    checked = state.defaultMode == RecordingMode.LOOP,
                    onCheckedChange = {
                        viewModel.defaultModeChanged(if (it) RecordingMode.LOOP else RecordingMode.NORMAL)
                    }
                )
                ToggleSection(
                    title = l10n.hudDisplay,
                    description = l10n.hudDisplayDesc,
                    checked = state.showHud,
                    onCheckedChange = viewModel::showHudChanged
                )
                ToggleSection(
                    title = l10n.generateSrtSubtitles,
                    description = l10n.generateSrtSubtitlesDesc,
                    checked = state.generateSrt,
                    onCheckedChange = viewModel::generateSrtChanged
                )
                LanguageSection(
                    selected = state.appLanguage,
                    onSelected = viewModel::appLanguageChanged,
                    l10n = l10n
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FloatingActionButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(64.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text(l10n.done, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(l10n.backToRecord, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ResolutionSection(
    selected: Resolution,
    onSelected: (Resolution) -> Unit,
    l10n: L10n
) {
    ChoiceCard(title = l10n.videoResolution) {
        Resolution.entries.forEach { resolution ->
            RadioRow(
                selected = selected == resolution,
                label = when (resolution) {
                    Resolution.HD_720P -> "720P (1280x720)"
                    Resolution.FHD_1080P -> "1080P (1920x1080)"
                    Resolution.UHD_4K -> "4K (3840x2160)"
                },
                onClick = { onSelected(resolution) }
            )
        }
    }
}

@Composable
private fun QualitySection(
    selected: VideoQuality,
    onSelected: (VideoQuality) -> Unit,
    l10n: L10n
) {
    ChoiceCard(title = l10n.videoQuality) {
        VideoQuality.entries.forEach { quality ->
            RadioRow(
                selected = selected == quality,
                label = when (quality) {
                    VideoQuality.LOW -> l10n.qualityLow
                    VideoQuality.MEDIUM -> l10n.qualityMedium
                    VideoQuality.HIGH -> l10n.qualityHigh
                    VideoQuality.VERY_HIGH -> l10n.qualityVeryHigh
                },
                onClick = { onSelected(quality) }
            )
        }
    }
}

@Composable
private fun LanguageSection(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    l10n: L10n
) {
    ChoiceCard(title = l10n.languageSetting) {
        RadioRow(
            selected = selected == AppLanguage.ZH,
            label = l10n.languageOptionZh,
            onClick = { onSelected(AppLanguage.ZH) }
        )
        RadioRow(
            selected = selected == AppLanguage.EN,
            label = l10n.languageOptionEn,
            onClick = { onSelected(AppLanguage.EN) }
        )
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun RadioRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.size(24.dp)
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ToggleSection(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SegmentDurationSection(
    seconds: Int,
    onChange: (Int) -> Unit,
    l10n: L10n
) {
    StepperCard(
        title = l10n.segmentDuration,
        value = "${seconds / 60} ${l10n.minutes}",
        onDecrease = { onChange(seconds - 60) },
        onIncrease = { onChange(seconds + 60) }
    )
}

@Composable
private fun MaxStorageSection(
    gb: Int,
    onChange: (Int) -> Unit,
    l10n: L10n
) {
    StepperCard(
        title = l10n.maxStorage,
        value = "$gb GB",
        onDecrease = { onChange(gb - 1) },
        onIncrease = { onChange(gb + 1) }
    )
}

@Composable
private fun StepperCard(
    title: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                    Text("-", style = MaterialTheme.typography.bodySmall)
                }
                Text(value, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                    Text("+", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
