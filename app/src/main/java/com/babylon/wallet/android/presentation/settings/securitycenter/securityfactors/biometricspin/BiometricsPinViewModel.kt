package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.factorsources.GetEntitiesLinkedToFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourceIntegrityStatusMessagesUseCase
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourcesOfTypeUseCase
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.utils.relativeTimeFormatted
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.supportsBabylon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

@HiltViewModel
class BiometricsPinViewModel @Inject constructor(
    getFactorSourcesOfTypeUseCase: GetFactorSourcesOfTypeUseCase,
    getEntitiesLinkedToFactorSourceUseCase: GetEntitiesLinkedToFactorSourceUseCase,
    getFactorSourceIntegrityStatusMessagesUseCase: GetFactorSourceIntegrityStatusMessagesUseCase,
    private val addFactorSourceProxy: AddFactorSourceProxy,
    private val preferencesManager: PreferencesManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<BiometricsPinViewModel.State>(),
    OneOffEventHandler<BiometricsPinViewModel.Event> by OneOffEventHandlerImpl() {

    init {
        combine(
            getFactorSourcesOfTypeUseCase<FactorSource.Device>(),
            preferencesManager.getBackedUpFactorSourceIds()
        ) { deviceFactorSources, _ ->
            _state.update { state ->
                state.copy(
                    factorSources = deviceFactorSources.mapNotNull { deviceFactorSource ->
                        val entitiesLinkedToDeviceFactorSource =
                            getEntitiesLinkedToFactorSourceUseCase(deviceFactorSource)
                                ?: return@mapNotNull null
                        val securityMessages =
                            getFactorSourceIntegrityStatusMessagesUseCase.forDeviceFactorSource(
                                deviceFactorSourceId = deviceFactorSource.id,
                                entitiesLinkedToDeviceFactorSource = entitiesLinkedToDeviceFactorSource
                            )
                        deviceFactorSource.value.toFactorSourceCard(
                            messages = securityMessages.toPersistentList(),
                            accounts = entitiesLinkedToDeviceFactorSource.accounts.toPersistentList(),
                            personas = entitiesLinkedToDeviceFactorSource.personas.toPersistentList(),
                            hasHiddenEntities = entitiesLinkedToDeviceFactorSource.hiddenAccounts.isNotEmpty() ||
                                entitiesLinkedToDeviceFactorSource.hiddenPersonas.isNotEmpty()
                        )
                    }.toPersistentList()
                )
            }
        }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)
    }

    override fun initialState(): State = State()

    fun onDeviceFactorSourceClick(factorSourceId: FactorSourceId) {
        viewModelScope.launch {
            sendEvent(Event.NavigateToDeviceFactorSourceDetails(factorSourceId))
        }
    }

    fun onAddBiometricsPinClick() {
        viewModelScope.launch {
            addFactorSourceProxy.addFactorSource(FactorSourceKind.DEVICE)
        }
    }

    private fun DeviceFactorSource.toFactorSourceCard(
        includeDescription: Boolean = false,
        messages: PersistentList<FactorSourceStatusMessage> = persistentListOf(),
        accounts: PersistentList<Account> = persistentListOf(),
        personas: PersistentList<Persona> = persistentListOf(),
        hasHiddenEntities: Boolean
    ): FactorSourceCard {
        return FactorSourceCard(
            id = id.asGeneral(),
            name = hint.label,
            includeDescription = includeDescription,
            lastUsedOn = common.lastUsedOn.relativeTimeFormatted(),
            kind = kind,
            messages = messages,
            accounts = accounts,
            personas = personas,
            hasHiddenEntities = hasHiddenEntities,
            supportsBabylon = this.asGeneral().supportsBabylon,
            isEnabled = true
        )
    }

    data class State(
        val factorSources: PersistentList<FactorSourceCard> = persistentListOf()
    ) : UiState

    sealed interface Event : OneOffEvent {

        data class NavigateToDeviceFactorSourceDetails(val factorSourceId: FactorSourceId) : Event
        data class NavigateToWriteDownSeedPhrase(val factorSourceId: FactorSourceId.Hash) : Event
        data object NavigateToSeedPhraseRestore : Event
    }
}
