package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
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
import com.radixdlt.sargon.EntitiesLinkedToFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceFlag
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIntegrity
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.ProfileToCheck
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BiometricsPinViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val preferencesManager: PreferencesManager,
    getProfileUseCase: GetProfileUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<BiometricsPinViewModel.State>(),
    OneOffEventHandler<BiometricsPinViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        getProfileUseCase.flow
            .flatMapConcat { profile ->
                profile.factorSources.asFlow()
            }
            .filterIsInstance<FactorSource.Device>()
            .map { deviceFactorSource ->
                val entitiesLinkedToDeviceFactorSource = sargonOsManager.sargonOs.entitiesLinkedToFactorSource(
                    factorSource = FactorSource.Device(deviceFactorSource.value),
                    profileToCheck = ProfileToCheck.Current
                )

                val securityMessages = getSecurityPromptsForDeviceFactorSource(
                    deviceFactorSourceId = deviceFactorSource.id,
                    entitiesLinkedToDeviceFactorSource = entitiesLinkedToDeviceFactorSource
                )

                val factorSourceCard = deviceFactorSource.value.toFactorSourceCard(
                    messages = securityMessages,
                    accounts = entitiesLinkedToDeviceFactorSource.accounts.toPersistentList(),
                    personas = entitiesLinkedToDeviceFactorSource.personas.toPersistentList(),
                    hasHiddenEntities = entitiesLinkedToDeviceFactorSource.hiddenAccounts.isNotEmpty() ||
                        entitiesLinkedToDeviceFactorSource.hiddenPersonas.isNotEmpty()
                )

                val isMainDeviceFactorSource = deviceFactorSource.value.common.flags.contains(FactorSourceFlag.MAIN)
                if (isMainDeviceFactorSource) {
                    _state.update { state ->
                        state.copy(mainDeviceFactorSource = factorSourceCard)
                    }
                } else {
                    _state.update { state ->
                        state.copy(
                            deviceFactorSources = state.deviceFactorSources.add(
                                factorSourceCard
                            )
                        )
                    }
                }
            }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)
    }

    @Suppress("UnusedParameter") // TODO
    fun onDeviceFactorSourceClick(hash: FactorSourceId.Hash) {
        viewModelScope.launch {
            sendEvent(Event.NavigateToDeviceFactorSourceDetails)
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
            hasHiddenEntities = hasHiddenEntities
        )
    }

    private suspend fun getSecurityPromptsForDeviceFactorSource(
        deviceFactorSourceId: FactorSourceId,
        entitiesLinkedToDeviceFactorSource: EntitiesLinkedToFactorSource
    ): PersistentList<FactorSourceStatusMessage> {
        val isDeviceFactorSourceLinkedToAnyEntities = listOf(
            entitiesLinkedToDeviceFactorSource.accounts,
            entitiesLinkedToDeviceFactorSource.personas,
            entitiesLinkedToDeviceFactorSource.hiddenAccounts,
            entitiesLinkedToDeviceFactorSource.hiddenPersonas
        ).any { it.isNotEmpty() }

        val backedUpFactorSourceIds = preferencesManager.getBackedUpFactorSourceIds().firstOrNull().orEmpty()

        return if (isDeviceFactorSourceLinkedToAnyEntities) {
            val deviceFactorSourceIntegrity = entitiesLinkedToDeviceFactorSource.integrity as FactorSourceIntegrity.Device
            deviceFactorSourceIntegrity.toMessages().toPersistentList()
        } else if (backedUpFactorSourceIds.contains(deviceFactorSourceId)) { // if not linked entities we can't check
            // the integrity, but we can check if the user backed up the seed phrase
            persistentListOf(FactorSourceStatusMessage.NoSecurityIssues)
        } else {
            // otherwise we don't show any warnings
            persistentListOf()
        }
    }

    private fun FactorSourceIntegrity.Device.toMessages(): List<FactorSourceStatusMessage> {
        val securityMessages = listOfNotNull(
            FactorSourceStatusMessage.SecurityPrompt.WriteDownSeedPhrase.takeIf {
                this.v1.isMnemonicMarkedAsBackedUp.not()
            },
            FactorSourceStatusMessage.SecurityPrompt.RecoveryRequired.takeIf {
                this.v1.isMnemonicPresentInSecureStorage.not()
            }
        )
        return securityMessages.ifEmpty {
            listOf(FactorSourceStatusMessage.NoSecurityIssues)
        }
    }

    data class State(
        val mainDeviceFactorSource: FactorSourceCard? = null,
        val deviceFactorSources: PersistentList<FactorSourceCard> = persistentListOf(),
    ) : UiState

    sealed interface Event : OneOffEvent {

        data object NavigateToDeviceFactorSourceDetails : Event
    }
}
