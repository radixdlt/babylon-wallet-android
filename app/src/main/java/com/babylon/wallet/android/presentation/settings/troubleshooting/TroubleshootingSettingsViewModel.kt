package com.babylon.wallet.android.presentation.settings.troubleshooting

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.babylon.wallet.android.utils.logger.PersistentLogger
import com.radixdlt.sargon.NetworkId
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class TroubleshootingSettingsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val deviceCapabilityHelper: DeviceCapabilityHelper,
    private val persistentLoggerProvider: Lazy<PersistentLogger>
) : StateViewModel<TroubleShootingUiState>() {

    override fun initialState(): TroubleShootingUiState {
        return TroubleShootingUiState(
            settings = listOf(
                TroubleshootingUiItem.RecoverySection,
                TroubleshootingUiItem.Setting(SettingsItem.Troubleshooting.AccountRecovery),
                TroubleshootingUiItem.Setting(SettingsItem.Troubleshooting.ImportFromLegacyWallet),
//                TroubleshootingUiItem.TransactionSection,
//                TroubleshootingUiItem.Setting(SettingsItem.Troubleshooting.SendTransactionManifest),
                TroubleshootingUiItem.SupportSection,
                TroubleshootingUiItem.Setting(
                    SettingsItem.Troubleshooting.ContactSupport(
                        body = deviceCapabilityHelper.supportEmailTemplate
                    )
                ),
                if (BuildConfig.FILE_LOGGER_ENABLED) {
                    TroubleshootingUiItem.Setting(SettingsItem.Troubleshooting.ExportLogs)
                } else {
                    null
                },
                TroubleshootingUiItem.ResetSection,
                TroubleshootingUiItem.Setting(SettingsItem.Troubleshooting.FactoryReset)
            ).filterNotNull().toPersistentSet()
        )
    }

    init {
        viewModelScope.launch {
            if (getProfileUseCase().currentGateway.network.id != NetworkId.MAINNET) {
                _state.update { state ->
                    val updatedSettings =
                        state.settings.filterNot {
                            it is TroubleshootingUiItem.Setting && it.item is SettingsItem.Troubleshooting.ImportFromLegacyWallet
                        }.toPersistentSet()
                    state.copy(
                        settings = updatedSettings
                    )
                }
            }
        }
    }

    fun onExportLogsToFile(file: Uri) = viewModelScope.launch {
        persistentLoggerProvider.get().exportToFile(file)
    }
}

sealed interface TroubleshootingUiItem {
    data object RecoverySection : TroubleshootingUiItem
    data object TransactionSection : TroubleshootingUiItem
    data object SupportSection : TroubleshootingUiItem
    data object ResetSection : TroubleshootingUiItem
    data class Setting(val item: SettingsItem.Troubleshooting) : TroubleshootingUiItem
}

data class TroubleShootingUiState(
    val settings: ImmutableSet<TroubleshootingUiItem>
) : UiState
