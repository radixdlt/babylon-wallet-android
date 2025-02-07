package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.babylon.wallet.android.presentation.accessfactorsources.applyshield.ApplyShieldViewModel.Event
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.toList
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ApplyShieldViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    private val incomingRequestRepository: IncomingRequestRepository
) : StateViewModel<ApplyShieldViewModel.State>(),
    OneOffEventHandler<ApplyShieldViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onApplyClick(
        securityStructureId: SecurityStructureId,
        entityAddresses: List<AddressOfAccountOrPersona>
    ) = viewModelScope.launch {
        _state.update { state -> state.copy(isLoading = true) }

        sargonOsManager.callSafely(dispatcher) {
            makeInteractionForApplyingSecurityShield(securityStructureId, entityAddresses)
        }.onSuccess { interaction ->
            Timber.d("Interaction: $interaction")
            // TODO prepare the batch transactions request and add it to the queue

            // This code is temporary
            if (interaction.transactions.size == 1) {
                val transaction = interaction.transactions.first()
                val request = UnvalidatedManifestData(
                    instructions = transaction.transactionManifestString,
                    plainMessage = null,
                    networkId = sargonOsManager.sargonOs.currentNetworkId(),
                    blobs = transaction.blobs.toList().map { it.bytes },
                ).prepareInternalTransactionRequest(
                    transactionType = TransactionType.SecurifyEntity(
                        entityAddress = entityAddresses.first() //
                    )
                )

                _state.update { state -> state.copy(isLoading = false) }
                sendEvent(Event.ShieldApplied)

                incomingRequestRepository.add(request)
            } else {
                // Batch request

                _state.update { state -> state.copy(isLoading = false) }
                sendEvent(Event.ShieldApplied)
            }
        }.onFailure { error ->
            _state.update { state ->
                state.copy(
                    isLoading = false,
                    message = UiMessage.ErrorMessage(error)
                )
            }
        }
    }

    fun onMessageShown() {
        _state.update { state -> state.copy(message = null) }
    }

    data class State(
        val isLoading: Boolean = false,
        val message: UiMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {

        data object ShieldApplied : Event
    }
}
