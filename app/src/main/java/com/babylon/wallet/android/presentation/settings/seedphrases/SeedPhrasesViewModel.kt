package com.babylon.wallet.android.presentation.settings.seedphrases

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.deviceFactorSourcesWithAccounts
import javax.inject.Inject

@HiltViewModel
class SeedPhrasesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) : StateViewModel<SeedPhrasesViewModel.SeedPhraseUiState>(),
    OneOffEventHandler<SeedPhrasesViewModel.Effect> by OneOffEventHandlerImpl() {

    override fun initialState() = SeedPhraseUiState()

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.deviceFactorSourcesWithAccounts,
                preferencesManager.getBackedUpFactorSourceIds()
            ) { factorSources, backedUpFactorSourceIds ->
                factorSources.map { entry ->
                    val mnemonic = mnemonicRepository.readMnemonic(entry.key.id)
                    val mnemonicState = when {
                        mnemonic == null -> DeviceFactorSourceData.MnemonicState.NeedRecover
                        backedUpFactorSourceIds.contains(entry.key.id.body.value) -> DeviceFactorSourceData.MnemonicState.BackedUp
                        else -> DeviceFactorSourceData.MnemonicState.NotBackedUp
                    }
                    DeviceFactorSourceData(
                        deviceFactorSource = entry.key,
                        accounts = entry.value.toPersistentList(),
                        mnemonicState = mnemonicState
                    )
                }
            }.collect { deviceFactorSources ->
                _state.update { it.copy(deviceFactorSourcesWithAccounts = deviceFactorSources.toPersistentList()) }
            }
        }
    }

    fun onSeedPhraseClick(deviceFactorSourceItem: DeviceFactorSourceData) {
        viewModelScope.launch {
            if (deviceFactorSourceItem.mnemonicState == DeviceFactorSourceData.MnemonicState.NeedRecover) {
                sendEvent(Effect.OnRequestToRecoverMnemonic(deviceFactorSourceItem.deviceFactorSource.id))
            } else {
                sendEvent(Effect.OnRequestToShowMnemonic(deviceFactorSourceItem.deviceFactorSource.id))
            }
        }
    }

    data class SeedPhraseUiState(
        val deviceFactorSourcesWithAccounts: PersistentList<DeviceFactorSourceData> = persistentListOf(),
    ) : UiState

    sealed interface Effect : OneOffEvent {
        data class OnRequestToShowMnemonic(val factorSourceID: FactorSourceID.FromHash) : Effect
        data class OnRequestToRecoverMnemonic(val factorSourceID: FactorSourceID.FromHash) : Effect
    }
}

data class DeviceFactorSourceData(
    val deviceFactorSource: DeviceFactorSource,
    val accounts: ImmutableList<Network.Account> = persistentListOf(),
    val mnemonicState: MnemonicState = MnemonicState.NotBackedUp
) {
    enum class MnemonicState {
        BackedUp, NotBackedUp, NeedRecover
    }
}
