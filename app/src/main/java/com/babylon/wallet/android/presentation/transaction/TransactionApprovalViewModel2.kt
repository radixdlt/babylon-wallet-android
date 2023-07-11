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
import com.babylon.wallet.android.domain.model.Badge
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetDAppWithMetadataAndAssociatedResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
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
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class TransactionApprovalViewModel2 @Inject constructor(
    private val transactionClient: TransactionClient,
    getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    getProfileUseCase: GetProfileUseCase,
    getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    getDAppWithMetadataAndAssociatedResourcesUseCase: GetDAppWithMetadataAndAssociatedResourcesUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val dAppMessenger: DappMessenger,
    @ApplicationScope private val appScope: CoroutineScope,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<State>(),
    OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = TransactionApprovalArgs(savedStateHandle)

    override fun initialState(): State = State(
        request = incomingRequestRepository.getTransactionWriteRequest(args.requestId),
        isLoading = true,
        previewType = PreviewType.NonConforming,
        isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
    )

    private var approvalJob: Job? = null

    private val analysis: TransactionAnalysisDelegate = TransactionAnalysisDelegate(
        state = _state,
        getProfileUseCase = getProfileUseCase,
        getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase,
        getTransactionBadgesUseCase = getTransactionBadgesUseCase,
        getDAppWithMetadataAndAssociatedResourcesUseCase = getDAppWithMetadataAndAssociatedResourcesUseCase,
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
        val isDeviceSecure: Boolean,
        val isLoading: Boolean,
        val isSigning: Boolean = false,
        val previewType: PreviewType,
        val fees: TransactionFees = TransactionFees(),
        val error: UiMessage? = null,
        val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom(),
        val networkFee: BigDecimal = TransactionConfig.NETWORK_FEE.toBigDecimal(),
        val signingState: SigningState? = null
    ): UiState {

        val message: String?
            get() {
                val message = request.transactionManifestData.message
                return if (!message.isNullOrBlank()) {
                    message
                } else {
                    null
                }
            }

    }

    sealed interface Event: OneOffEvent {
        object Dismiss : Event
        object SelectFeePayer : Event
    }
}

sealed interface PreviewType {
    object NonConforming: PreviewType

    data class Transaction(
        val from: List<AccountWithTransferableResources>,
        val to: List<AccountWithTransferableResources>,
        val badges: List<Badge> = emptyList()
    ): PreviewType
}

sealed interface AccountWithTransferableResources {

    val address: String
    val resources: List<Transferable>

    data class Owned(
        val account: Network.Account,
        override val resources: List<Transferable>
    ): AccountWithTransferableResources {
        override val address: String
            get() = account.address
    }

    data class Other(
        override val address: String,
        override val resources: List<Transferable>
    ): AccountWithTransferableResources
}

data class TransactionFees(
    val networkFee: BigDecimal = BigDecimal.ZERO,
    val isNetworkCongested: Boolean = false
)



