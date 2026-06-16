package com.e9ab98.kmprecording.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.e9ab98.kmprecording.domain.IOSSettingsRepository
import com.e9ab98.kmprecording.presentation.SettingsViewModel

@Composable
actual fun SettingsScreenActual(
    onNavigateBack: () -> Unit
) {
    val settingsRepository = remember { IOSSettingsRepository() }
    val viewModel = remember { SettingsViewModel(settingsRepository) }

    SettingsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack
    )
}
