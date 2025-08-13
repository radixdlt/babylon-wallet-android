package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.offdevicemnemonic

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourcesOfTypeUseCase
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.ProfileToCheck
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OffDeviceMnemonicsViewModel @Inject constructor(
    getFactorSourcesOfTypeUseCase: GetFactorSourcesOfTypeUseCase,
    private val sargonOsManager: SargonOsManager,
    private val addFactorSourceProxy: AddFactorSourceProxy,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<OffDeviceMnemonicsViewModel.State>(),
    OneOffEventHandler<OffDeviceMnemonicsViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        @Suppress("OPT_IN_USAGE")
        getFactorSourcesOfTypeUseCase<FactorSource.OffDeviceMnemonic>()
            .mapLatest { offDeviceMnemonics ->
                resetOffDeviceMnemonicFactorSourceList()

                offDeviceMnemonics.map { offDeviceMnemonic ->
                    sargonOsManager.callSafely(dispatcher = defaultDispatcher) {
                        entitiesLinkedToFactorSource(
                            factorSource = FactorSource.OffDeviceMnemonic(offDeviceMnemonic.value),
                            profileToCheck = ProfileToCheck.Current
                        )
                    }.onSuccess { entitiesLinkedToOffDeviceMnemonicFactorSource ->
                        val factorSourceCard = offDeviceMnemonic.value.asGeneral().toFactorSourceCard(
                            messages = persistentListOf(),
                            accounts = entitiesLinkedToOffDeviceMnemonicFactorSource.accounts.toPersistentList(),
                            personas = entitiesLinkedToOffDeviceMnemonicFactorSource.personas.toPersistentList(),
                            hasHiddenEntities = entitiesLinkedToOffDeviceMnemonicFactorSource.hiddenAccounts.isNotEmpty() ||
                                entitiesLinkedToOffDeviceMnemonicFactorSource.hiddenPersonas.isNotEmpty()
                        )
                        // avoid duplication when a factor source is updated in the Factor Source Details screen
                        val updatedOffDeviceMnemonicFactorSources = _state.value.offDeviceMnemonicFactorSources
                            .filterNot { it.id == factorSourceCard.id }
                            .toMutableList()
                        updatedOffDeviceMnemonicFactorSources.add(factorSourceCard)
                        _state.update { state ->
                            state.copy(offDeviceMnemonicFactorSources = updatedOffDeviceMnemonicFactorSources.toPersistentList())
                        }
                    }.onFailure { error ->
                        Timber.e("Failed to find linked entities: $error")
                    }
                }
            }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)
    }

    private fun resetOffDeviceMnemonicFactorSourceList() {
        _state.update { state -> state.copy(offDeviceMnemonicFactorSources = persistentListOf()) }
    }

    fun onOffDeviceMnemonicFactorSourceClick(factorSourceId: FactorSourceId) {
        viewModelScope.launch {
            sendEvent(Event.NavigateToOffDeviceMnemonicFactorSourceDetails(factorSourceId))
        }
    }

    fun onAddOffDeviceMnemonicClick() {
        viewModelScope.launch {
            addFactorSourceProxy.addFactorSource(
                AddFactorSourceInput.WithKind(
                    kind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
                    context = AddFactorSourceInput.Context.New
                )
            )
        }
    }

    data class State(
        val offDeviceMnemonicFactorSources: PersistentList<FactorSourceCard> = persistentListOf(),
    ) : UiState

    sealed interface Event : OneOffEvent {

        data class NavigateToOffDeviceMnemonicFactorSourceDetails(val factorSourceId: FactorSourceId) : Event
    }
}
