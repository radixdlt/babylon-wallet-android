package com.babylon.wallet.android.presentation.accessfactorsources.applyshield

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.SecurityStructureMetadata
import com.radixdlt.sargon.extensions.blobs
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.toList
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ApplyShieldViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val sargonOsManager: SargonOsManager,
    private val incomingRequestRepository: IncomingRequestRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) : StateViewModel<ApplyShieldViewModel.State>(),
    OneOffEventHandler<ApplyShieldViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(
        address = ApplyShieldArgs(savedStateHandle).address
    )

    init {
        viewModelScope.launch {
            val metadata = getProfileUseCase().appPreferences.security.securityStructuresOfFactorSourceIds.map {
                it.metadata
            }

            _state.update { it.copy(shields = metadata) }
        }
    }

    fun onSelected(id: SecurityStructureId) {
        viewModelScope.launch {
            _state.update { it.copy(selected = id) }
        }
    }

    fun onApply() {
        val shield = state.value.shields.find { it.id == state.value.selected } ?: return

        applicationScope.launch {
            _state.update { it.copy(isApplying = true) }
            withContext(defaultDispatcher) {
                val networkId = getProfileUseCase().currentGateway.network.id
                runCatching {
                    val batchOfTransactions = sargonOsManager.sargonOs.makeInteractionForApplyingSecurityShield(
                        securityShieldId = shield.id,
                        addresses = listOf(
                            _state.value.address
                        )
                    )

                    val transaction = batchOfTransactions.transactions.first()
                    UnvalidatedManifestData(
                        instructions = transaction.transactionManifestString,
                        plainMessage = null,
                        networkId = networkId,
                        blobs = transaction.blobs.toList().map { it.bytes },
                    ).prepareInternalTransactionRequest()
                }.onSuccess { request ->
                    _state.update { it.copy(isApplying = false) }
                    sendEvent(Event.Dismiss)

                    incomingRequestRepository.add(request)
                }.onFailure { error ->
                    _state.update { it.copy(isApplying = false) }
                    Timber.w(error)
                }
            }
        }
    }

    data class State(
        val address: AddressOfAccountOrPersona,
        val selected: SecurityStructureId? = null,
        val shields: List<SecurityStructureMetadata> = emptyList(),
        val isApplying: Boolean = false
    ) : UiState {

        val isApplyEnabled: Boolean
            get() = shields.isNotEmpty() && selected != null
    }

    sealed interface Event : OneOffEvent {
        data object Dismiss : Event
    }
}
