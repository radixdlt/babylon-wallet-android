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
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.deviceFactorSourcesWithAccounts
import javax.inject.Inject

@HiltViewModel
class SeedPhrasesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
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
                    DeviceFactorSourceData(entry.key, entry.value.toPersistentList(), backedUpFactorSourceIds.contains(entry.key.value))
                }
            }.collect { deviceFactorSources ->
                _state.update { it.copy(deviceFactorSourcesWithAccounts = deviceFactorSources.toPersistentList()) }
            }
        }
    }

    fun onShowMnemonic(factorSourceID: FactorSource.ID) {
        viewModelScope.launch {
            sendEvent(Effect.OnRequestToShowMnemonic(factorSourceID))
        }
    }

    data class SeedPhraseUiState(
        val deviceFactorSourcesWithAccounts: PersistentList<DeviceFactorSourceData> = persistentListOf(),
    ) : UiState

    sealed interface Effect : OneOffEvent {
        data class OnRequestToShowMnemonic(val factorSourceID: FactorSource.ID) : Effect
        data class OnRequestToRecoverMnemonic(val accountAddress: String) : Effect
    }
}

data class DeviceFactorSourceData(
    val id: FactorSource.ID,
    val accounts: ImmutableList<Network.Account> = persistentListOf(),
    val backedUp: Boolean = false
)
