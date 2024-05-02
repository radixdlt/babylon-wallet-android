package com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.asGeneral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.DeviceFactorSourceData
import rdx.works.profile.domain.GetFactorSourcesWithAccountsUseCase
import javax.inject.Inject

@HiltViewModel
class SeedPhrasesViewModel @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager,
    private val getFactorSourcesWithAccountsUseCase: GetFactorSourcesWithAccountsUseCase
) : StateViewModel<SeedPhrasesViewModel.SeedPhraseUiState>(),
    OneOffEventHandler<SeedPhrasesViewModel.Effect> by OneOffEventHandlerImpl() {

    override fun initialState() = SeedPhraseUiState()
    private val refreshFlow = MutableSharedFlow<Unit>()

    init {
        viewModelScope.launch {
            combine(
                getFactorSourcesWithAccountsUseCase(),
                refreshFlow
            ) { factorSources, _ ->
                val backedUpFactorSourceIds = preferencesManager.getBackedUpFactorSourceIds().firstOrNull().orEmpty()
                factorSources.map { data ->
                    val mnemonicExist = mnemonicRepository.mnemonicExist(data.deviceFactorSource.value.id.asGeneral())
                    val mnemonicState = when {
                        !mnemonicExist -> DeviceFactorSourceData.MnemonicState.NeedRecover
                        backedUpFactorSourceIds.contains(
                            data.deviceFactorSource.value.id.asGeneral()
                        ) -> DeviceFactorSourceData.MnemonicState.BackedUp
                        else -> DeviceFactorSourceData.MnemonicState.NotBackedUp
                    }
                    data.copy(mnemonicState = mnemonicState)
                }
            }.collect { deviceFactorSources ->
                _state.update { it.copy(deviceFactorSourcesWithAccounts = deviceFactorSources.toPersistentList()) }
            }
        }
        viewModelScope.launch {
            preferencesManager.getBackedUpFactorSourceIds().collect {
                refreshFlow.emit(Unit)
            }
        }
    }

    fun onSeedPhraseClick(deviceFactorSourceItem: DeviceFactorSourceData) {
        viewModelScope.launch {
            if (deviceFactorSourceItem.mnemonicState == DeviceFactorSourceData.MnemonicState.NeedRecover) {
                sendEvent(Effect.OnRequestToRecoverMnemonic(deviceFactorSourceItem.deviceFactorSource.value.id.asGeneral()))
            } else {
                sendEvent(Effect.OnRequestToShowMnemonic(deviceFactorSourceItem.deviceFactorSource.value.id.asGeneral()))
            }
        }
    }

    data class SeedPhraseUiState(
        val deviceFactorSourcesWithAccounts: PersistentList<DeviceFactorSourceData> = persistentListOf(),
    ) : UiState

    sealed interface Effect : OneOffEvent {
        data class OnRequestToShowMnemonic(val factorSourceID: FactorSourceId.Hash) : Effect
        data class OnRequestToRecoverMnemonic(val factorSourceID: FactorSourceId.Hash) : Effect
    }
}
