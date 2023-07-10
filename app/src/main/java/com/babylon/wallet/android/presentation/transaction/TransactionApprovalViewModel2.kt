package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.SigningState
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.model.AccountWithTransferableResources
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.usecases.GetDAppWithMetadataAndAssociatedResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionResourcesFromAnalysis
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionProofResourcesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2.Event
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2.State
import com.babylon.wallet.android.presentation.transaction.analysis.TransactionAnalysisDelegate
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.crypto.PrivateKey
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class TransactionApprovalViewModel2 @Inject constructor(
    private val transactionClient: TransactionClient,
    private val getTransactionResourcesFromAnalysis: GetTransactionResourcesFromAnalysis,
    private val getTransactionProofResourcesUseCase: GetTransactionProofResourcesUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val dAppMessenger: DappMessenger,
    private val dAppWithAssociatedResourcesUseCase: GetDAppWithMetadataAndAssociatedResourcesUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<State>(),
    OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = TransactionApprovalArgs(savedStateHandle)

    override fun initialState(): State = State(
        request = incomingRequestRepository.getTransactionWriteRequest(args.requestId),
        ephemeralNotaryPrivateKey = PrivateKey.EddsaEd25519.newRandom(),
        isLoading = true,
        previewType = PreviewType.NonConforming,
        isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
    )

    private var approvalJob: Job? = null

    private val analysis: TransactionAnalysisDelegate = TransactionAnalysisDelegate(
        state = _state,
        getTransactionResourcesFromAnalysis = getTransactionResourcesFromAnalysis,
        transactionClient = transactionClient
    )

    init {
        viewModelScope.launch {
            transactionClient.signingState.filterNotNull().collect { signingState ->
                _state.update { state ->
                    state.copy(signingState = signingState)
                }
            }
        }

        viewModelScope.launch {
            analysis.analyse()
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            dismissTransaction(DappRequestFailure.RejectedByUser)
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun dismissTransaction(failure: DappRequestFailure) {
        if (approvalJob == null) {
            val request = state.value.request
            if (!request.isInternal) {
                dAppMessenger.sendWalletInteractionResponseFailure(
                    dappId = request.dappId,
                    requestId = args.requestId,
                    error = failure.toWalletErrorType(),
                    message = failure.getDappMessage()
                )
            }
            sendEvent(Event.Dismiss)
            incomingRequestRepository.requestHandled(args.requestId)
        } else {
            Timber.d("Cannot dismiss transaction while is in progress")
        }
    }

    data class State(
        val request: MessageFromDataChannel.IncomingRequest.TransactionRequest,
        val ephemeralNotaryPrivateKey: PrivateKey,
        val isDeviceSecure: Boolean,
        val isLoading: Boolean,
        val previewType: PreviewType,
        val error: UiMessage? = null,
        val networkFee: BigDecimal = TransactionConfig.NETWORK_FEE.toBigDecimal(),
        val signingState: SigningState? = null
    ): UiState

    sealed interface Event: OneOffEvent {
        object Dismiss : Event
        object SelectFeePayer : Event
    }
}

sealed interface PreviewType {
    object NonConforming: PreviewType

    data class Transfer(
        val from: List<AccountWithTransferableResources>,
        val to: List<AccountWithTransferableResources>
    ): PreviewType
}



