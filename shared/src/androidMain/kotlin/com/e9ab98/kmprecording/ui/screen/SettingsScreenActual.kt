package com.e9ab98.kmprecording.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.e9ab98.kmprecording.domain.AndroidSettingsRepository
import com.e9ab98.kmprecording.presentation.SettingsViewModel

@Composable
actual fun SettingsScreenActual(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepository = remember { AndroidSettingsRepository(context) }
    val viewModel = remember { SettingsViewModel(settingsRepository) }

    SettingsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack
    )
}
