package com.babylon.wallet.android.presentation.settings.troubleshooting

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.radixdlt.sargon.NetworkId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@Suppress("MagicNumber")
@HiltViewModel
class TroubleshootingSettingsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<TroubleShootingUiState>() {

    override fun initialState(): TroubleShootingUiState = TroubleShootingUiState(
        settings = defaultSettings
    )

    init {
        viewModelScope.launch {
            if (getProfileUseCase().currentGateway.network.id != NetworkId.MAINNET || !BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
                _state.update { state ->
                    val updatedSettings =
                        defaultSettings.filterNot {
                            it is TroubleshootingUiItem.Setting && it.item is SettingsItem.Troubleshooting.ImportFromLegacyWallet
                        }.toPersistentSet()
                    state.copy(
                        settings = updatedSettings
                    )
                }
            }
        }
    }

    companion object {
        private val defaultSettings: ImmutableSet<TroubleshootingUiItem> = persistentSetOf(
            TroubleshootingUiItem.RecoverySection,
            TroubleshootingUiItem.Setting(SettingsItem.Troubleshooting.AccountRecovery),
            TroubleshootingUiItem.Setting(SettingsItem.Troubleshooting.ImportFromLegacyWallet),
            TroubleshootingUiItem.SupportSection,
            TroubleshootingUiItem.Setting(SettingsItem.Troubleshooting.ContactSupport),
            TroubleshootingUiItem.Setting(SettingsItem.Troubleshooting.Discord),
            TroubleshootingUiItem.ResetSection,
            TroubleshootingUiItem.Setting(SettingsItem.Troubleshooting.FactoryReset)
        )
    }
}

sealed interface TroubleshootingUiItem {
    data object RecoverySection : TroubleshootingUiItem
    data object SupportSection : TroubleshootingUiItem
    data object ResetSection : TroubleshootingUiItem
    data class Setting(val item: SettingsItem.Troubleshooting) : TroubleshootingUiItem
}

data class TroubleShootingUiState(
    val settings: ImmutableSet<TroubleshootingUiItem>
) : UiState
