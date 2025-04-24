package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
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
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage.SecurityPrompt
import com.babylon.wallet.android.utils.callSafely
import com.babylon.wallet.android.utils.relativeTimeFormatted
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceFlag
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.supportsBabylon
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BiometricsPinViewModel @Inject constructor(
    getFactorSourcesOfTypeUseCase: GetFactorSourcesOfTypeUseCase,
    getEntitiesLinkedToFactorSourceUseCase: GetEntitiesLinkedToFactorSourceUseCase,
    getFactorSourceIntegrityStatusMessagesUseCase: GetFactorSourceIntegrityStatusMessagesUseCase,
    private val biometricsAuthenticateUseCase: BiometricsAuthenticateUseCase,
    private val sargonOsManager: SargonOsManager,
    private val addFactorSourceProxy: AddFactorSourceProxy,
    private val preferencesManager: PreferencesManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<BiometricsPinViewModel.State>(),
    OneOffEventHandler<BiometricsPinViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        combine(
            getFactorSourcesOfTypeUseCase<FactorSource.Device>(),
            preferencesManager.getBackedUpFactorSourceIds()
        ) { deviceFactorSources, _ ->
            resetDeviceFactorSourceList()

            deviceFactorSources.forEach { deviceFactorSource ->
                val entitiesLinkedToDeviceFactorSource =
                    getEntitiesLinkedToFactorSourceUseCase(deviceFactorSource)
                        ?: return@forEach
                val securityMessages =
                    getFactorSourceIntegrityStatusMessagesUseCase.forDeviceFactorSource(
                        deviceFactorSourceId = deviceFactorSource.id,
                        entitiesLinkedToDeviceFactorSource = entitiesLinkedToDeviceFactorSource
                    )
                val factorSourceCard = deviceFactorSource.value.toFactorSourceCard(
                    messages = securityMessages.toPersistentList(),
                    accounts = entitiesLinkedToDeviceFactorSource.accounts.toPersistentList(),
                    personas = entitiesLinkedToDeviceFactorSource.personas.toPersistentList(),
                    hasHiddenEntities = entitiesLinkedToDeviceFactorSource.hiddenAccounts.isNotEmpty() ||
                        entitiesLinkedToDeviceFactorSource.hiddenPersonas.isNotEmpty()
                )
                val isMainDeviceFactorSource =
                    deviceFactorSource.value.common.flags.contains(FactorSourceFlag.MAIN)

                if (isMainDeviceFactorSource) {
                    _state.update { state ->
                        state.copy(mainDeviceFactorSource = factorSourceCard)
                    }
                } else {
                    _state.update { state ->
                        state.copy(
                            otherDeviceFactorSources = state.otherDeviceFactorSources.add(
                                factorSourceCard
                            )
                        )
                    }
                }
            }
        }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)
    }

    private fun resetDeviceFactorSourceList() {
        _state.update { state ->
            state.copy(
                mainDeviceFactorSource = null,
                otherDeviceFactorSources = persistentListOf()
            )
        }
    }

    fun onDeviceFactorSourceClick(factorSourceId: FactorSourceId) {
        viewModelScope.launch {
            sendEvent(Event.NavigateToDeviceFactorSourceDetails(factorSourceId))
        }
    }

    fun onSecurityPromptMessageClicked(id: FactorSourceId, message: SecurityPrompt) {
        val hashFactorSourceId = id as? FactorSourceId.Hash ?: return
        when (message) {
            SecurityPrompt.EntitiesNotRecoverable,
            SecurityPrompt.WriteDownSeedPhrase -> viewModelScope.launch {
                if (biometricsAuthenticateUseCase()) {
                    sendEvent(Event.NavigateToWriteDownSeedPhrase(hashFactorSourceId))
                }
            }
            SecurityPrompt.LostFactorSource,
            SecurityPrompt.SeedPhraseNeedRecovery -> viewModelScope.launch {
                sendEvent(Event.NavigateToSeedPhraseRestore)
            }
        }
    }

    fun onChangeMainDeviceFactorSourceClick() {
        _state.update { state -> state.copy(isMainDeviceFactorSourceBottomSheetVisible = true) }
    }

    fun onDeviceFactorSourceSelect(factorSourceCard: FactorSourceCard) {
        _state.update { it.copy(selectedDeviceFactorSourceId = factorSourceCard.id) }
    }

    fun onConfirmChangeMainDeviceFactorSource() {
        viewModelScope.launch {
            _state.update { state -> state.copy(isChangingMainDeviceFactorSourceInProgress = true) }
            _state.value.selectedDeviceFactorSourceId?.let { id ->
                sargonOsManager.callSafely(defaultDispatcher) {
                    setMainFactorSource(factorSourceId = id)
                }.onFailure { error ->
                    Timber.e("Failed to set main device factor source: $error")
                }
            }
            _state.update { state -> state.copy(isChangingMainDeviceFactorSourceInProgress = false) }
            onDismissMainDeviceFactorSourceBottomSheet()
        }
    }

    fun onAddBiometricsPinClick() {
        viewModelScope.launch {
            addFactorSourceProxy.addFactorSource(FactorSourceKind.DEVICE)
        }
    }

    fun onDismissMainDeviceFactorSourceBottomSheet() {
        _state.update { state ->
            state.copy(
                isMainDeviceFactorSourceBottomSheetVisible = false,
                selectedDeviceFactorSourceId = null
            )
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
        val mainDeviceFactorSource: FactorSourceCard? = null,
        val otherDeviceFactorSources: PersistentList<FactorSourceCard> = persistentListOf(),
        val isMainDeviceFactorSourceBottomSheetVisible: Boolean = false,
        val isChangingMainDeviceFactorSourceInProgress: Boolean = false,
        val selectedDeviceFactorSourceId: FactorSourceId? = null,
    ) : UiState {

        val isContinueButtonEnabled: Boolean
            get() = selectableDeviceFactorIds.any { it.selected }

        val selectableDeviceFactorIds: ImmutableList<Selectable<FactorSourceCard>> =
            otherDeviceFactorSources
                .filter { it.supportsBabylon }
                .map {
                    Selectable(
                        data = it,
                        selected = selectedDeviceFactorSourceId == it.id
                    )
                }.toImmutableList()
    }

    sealed interface Event : OneOffEvent {

        data class NavigateToDeviceFactorSourceDetails(val factorSourceId: FactorSourceId) : Event
        data class NavigateToWriteDownSeedPhrase(val factorSourceId: FactorSourceId.Hash) : Event
        data object NavigateToSeedPhraseRestore : Event
    }
}
