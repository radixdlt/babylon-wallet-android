package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.settings.SettingsItem.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.profile.domain.preferences.IsInDeveloperModeUseCase
import rdx.works.profile.domain.preferences.UpdateDeveloperModeUseCase
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val isInDeveloperModeUseCase: IsInDeveloperModeUseCase,
    private val updateDeveloperModeUseCase: UpdateDeveloperModeUseCase
) : ViewModel() {

    private val _state: MutableStateFlow<SettingsUiState> = MutableStateFlow(SettingsUiState.default)
    val state = _state.asStateFlow()

    init {
        readSettings()
    }

    private fun readSettings() {
        viewModelScope.launch {
            val isInDeveloperMode = isInDeveloperModeUseCase()
            _state.updateSetting<AppSettings.DeveloperMode> {
                AppSettings.DeveloperMode(isInDeveloperMode)
            }
        }
    }

    fun onDeveloperModeToggled(enabled: Boolean) = viewModelScope.launch {
        updateDeveloperModeUseCase(isEnabled = enabled)
        _state.updateSetting<AppSettings.DeveloperMode> {
            AppSettings.DeveloperMode(enabled)
        }
    }

    private inline fun <reified S : AppSettings> MutableStateFlow<SettingsUiState>.updateSetting(
        mutation: (S) -> S
    ) = update { uiState ->
        uiState.copy(
            settings = uiState.settings.mapWhen(
                predicate = { it is S },
                mutation = { mutation(it as S) }
            ).toPersistentSet()
        )
    }
}

data class SettingsUiState(
    val settings: ImmutableSet<AppSettings>
) {

    companion object {
        val default = SettingsUiState(
            settings = persistentSetOf(AppSettings.DeveloperMode(false))
        )
    }
}
