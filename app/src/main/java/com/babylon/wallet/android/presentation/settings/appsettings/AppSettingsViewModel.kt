package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.security
import rdx.works.profile.domain.security.UpdateDeveloperModeUseCase
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateDeveloperModeUseCase: UpdateDeveloperModeUseCase
) : StateViewModel<AppSettingsUiState>() {

    override fun initialState(): AppSettingsUiState = AppSettingsUiState.default

    init {
        readSettings()
    }

    private fun readSettings() {
        viewModelScope.launch {
            getProfileUseCase
                .security
                .map { it.isDeveloperModeEnabled }
                .collect { isInDeveloperMode ->
                    _state.updateSetting<SettingsItem.AppSettingsItem.DeveloperMode> {
                        SettingsItem.AppSettingsItem.DeveloperMode(isInDeveloperMode)
                    }
                }
        }
    }

    fun onDeveloperModeToggled(enabled: Boolean) = viewModelScope.launch {
        updateDeveloperModeUseCase(isEnabled = enabled)
    }

    private inline fun <reified S : SettingsItem.AppSettingsItem> MutableStateFlow<AppSettingsUiState>.updateSetting(
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

data class AppSettingsUiState(
    val settings: ImmutableSet<SettingsItem.AppSettingsItem>
) : UiState {

    companion object {
        val default = AppSettingsUiState(
            settings = persistentSetOf(
                SettingsItem.AppSettingsItem.LinkedConnectors,
                SettingsItem.AppSettingsItem.Gateways,
                SettingsItem.AppSettingsItem.EntityHiding,
                SettingsItem.AppSettingsItem.DeveloperMode(false)
            )
        )
    }
}
