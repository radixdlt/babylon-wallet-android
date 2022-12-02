package com.babylon.wallet.android.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    var state by mutableStateOf(SettingsUiState())
        private set
}

data class SettingsUiState(
    val settings: ImmutableList<SettingSection> = defaultAppSettings
)
