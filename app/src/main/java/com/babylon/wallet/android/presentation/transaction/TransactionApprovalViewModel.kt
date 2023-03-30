package com.babylon.wallet.android.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.toPrettyString
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionComponentResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionProofResourcesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import com.radixdlt.toolkit.models.request.AccountDeposit
import com.radixdlt.toolkit.models.request.ResourceSpecifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import rdx.works.core.decodeHex
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class TransactionApprovalViewModel @Inject constructor(
    private val transactionClient: TransactionClient,
    private val getTransactionComponentResourcesUseCase: GetTransactionComponentResourcesUseCase,
    private val getTransactionProofResourcesUseCase: GetTransactionProofResourcesUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val profileDataSource: ProfileDataSource,
    deviceSecurityHelper: DeviceSecurityHelper,
    private val dAppMessenger: DappMessenger,
    @ApplicationScope private val appScope: CoroutineScope,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), OneOffEventHandler<TransactionApprovalEvent> by OneOffEventHandlerImpl() {

    private val args = TransactionApprovalArgs(savedStateHandle)
    private val transactionWriteRequest = incomingRequestRepository.getTransactionWriteRequest(args.requestId)

    internal var state by mutableStateOf(TransactionUiState(isDeviceSecure = deviceSecurityHelper.isDeviceSecure()))
        private set

    private var approvalJob: Job? = null

    init {
        viewModelScope.launch {
            state = state.copy(
                manifestData = transactionWriteRequest.transactionManifestData,
                transactionMessage = transactionWriteRequest.transactionManifestData.message.orEmpty()
            )
            val manifestResult = transactionClient.addLockFeeToTransactionManifestData(
                transactionWriteRequest.transactionManifestData
            )

            manifestResult.onValue { manifestWithLockFee ->
                when (
                    val manifestInStringFormatConversionResult = transactionClient.manifestInStringFormat(
                        manifestWithLockFee
                    )
                ) {
                    is Result.Error -> {
                        state = state.copy(
                            isLoading = false,
                            error = UiMessage.ErrorMessage(manifestInStringFormatConversionResult.exception)
                        )
                    }
                    is Result.Success -> {
                        val transactionManifest = manifestInStringFormatConversionResult.data
                        val transactionPreview = transactionClient.getTransactionPreview(
                            manifest = transactionManifest,
                            networkId = profileDataSource.getCurrentNetworkId().value
                        )
                        transactionPreview.onError {
                            state = state.copy(
                                isLoading = false,
                                error = UiMessage.ErrorMessage(it)
                            )
                        }
                        transactionPreview.onValue { transactionPreviewResponse ->
                            transactionPreviewResponse.receipt.fee_summary.let { feeSummary ->
                                val costUnitPrice = feeSummary.cost_unit_price.toBigDecimal()
                                val costUnitsConsumed = feeSummary.cost_units_consumed.toBigDecimal()
                                val networkFee = costUnitPrice.multiply(costUnitsConsumed).toString()
                                state = state.copy(
                                    networkFee = networkFee
                                )
                            }

                            val manifestPreview = transactionClient.analyzeManifest(
                                networkId = profileDataSource.getCurrentNetworkId(),
                                transactionManifest = transactionManifest,
                                transactionReceipt = transactionPreviewResponse.encodedReceipt.decodeHex()
                            )

                            manifestPreview.exceptionOrNull().let {
                                state = state.copy(
                                    isLoading = false,
                                    error = UiMessage.ErrorMessage(it)
                                )
                            }

                            manifestPreview.getOrNull()?.let { analyzeManifestWithPreviewResponse ->

                                val depositJobs: MutableList<Deferred<Result<TransactionAccountItemUiModel>>> = mutableListOf()
                                val withdrawJobs: MutableList<Deferred<Result<TransactionAccountItemUiModel>>> = mutableListOf()

                                analyzeManifestWithPreviewResponse.accountDeposits.forEach {
                                    when (it) {
                                        is AccountDeposit.Estimate -> {
                                            val accountDepositResourceSpecifier =
                                                (it.resourceSpecifier as ResourceSpecifier.Amount)

                                            depositJobs.add(
                                                async {
                                                    getTransactionComponentResourcesUseCase.invoke(
                                                        componentAddresses = listOf(
                                                            it.componentAddress.address,
                                                            accountDepositResourceSpecifier.resourceAddress.address
                                                        ),
                                                        amount = accountDepositResourceSpecifier.amount
                                                    )
                                                }
                                            )
                                        }
                                        is AccountDeposit.Exact -> {
                                            val accountDepositResourceSpecifier =
                                                (it.resourceSpecifier as ResourceSpecifier.Amount)
                                            depositJobs.add(
                                                async {
                                                    getTransactionComponentResourcesUseCase.invoke(
                                                        componentAddresses = listOf(
                                                            it.componentAddress.address,
                                                            accountDepositResourceSpecifier.resourceAddress.address
                                                        ),
                                                        amount = accountDepositResourceSpecifier.amount
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                                analyzeManifestWithPreviewResponse.accountWithdraws.forEach {
                                    val accountWithdrawResourceSpecifier =
                                        (it.resourceSpecifier as ResourceSpecifier.Amount)
                                    withdrawJobs.add(
                                        async {
                                            getTransactionComponentResourcesUseCase.invoke(
                                                componentAddresses = listOf(
                                                    it.componentAddress.address,
                                                    accountWithdrawResourceSpecifier.resourceAddress.address
                                                ),
                                                amount = accountWithdrawResourceSpecifier.amount
                                            )
                                        }
                                    )
                                }

                                val depositResults = depositJobs.awaitAll()
                                val withdrawResults = withdrawJobs.awaitAll()

                                val withdrawingAccounts = withdrawResults
                                    .filterIsInstance<Result.Success<TransactionAccountItemUiModel>>()
                                    .map {
                                        it.data
                                    }

                                val depositingAccounts = depositResults
                                    .filterIsInstance<Result.Success<TransactionAccountItemUiModel>>()
                                    .map {
                                        it.data
                                    }

                                val proofs = getTransactionProofResourcesUseCase(
                                    analyzeManifestWithPreviewResponse.accountProofResources
                                )

                                state = state.copy(
                                    withdrawingAccounts = withdrawingAccounts.toPersistentList(),
                                    depositingAccounts = depositingAccounts.toPersistentList(),
                                    presentingProofs = proofs.toPersistentList(),
                                    connectedDApps = persistentListOf(), // TODO something to come later on
                                )
                            }
                        }

                        state = state.copy(
                            manifestString = manifestInStringFormatConversionResult.data.toPrettyString(),
                            manifestData = transactionWriteRequest.transactionManifestData,
                            canApprove = true,
                            isLoading = false
                        )
                    }
                }
            }
            manifestResult.onError { error ->
                state = state.copy(
                    isLoading = false,
                    error = UiMessage.ErrorMessage(error)
                )
                dAppMessenger.sendWalletInteractionResponseFailure(
                    dappId = transactionWriteRequest.dappId,
                    requestId = args.requestId,
                    error = WalletErrorType.FailedToFindAccountWithEnoughFundsToLockFee
                )
            }
        }
    }

    @Suppress("LongMethod")
    fun approveTransaction() {
        approvalJob = appScope.launch {
            state.manifestData?.let { manifestData ->
                val currentNetworkId = profileDataSource.getCurrentNetworkId().value
                if (currentNetworkId != manifestData.networkId) {
                    val failure = DappRequestFailure.WrongNetwork(currentNetworkId, manifestData.networkId)
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        dappId = transactionWriteRequest.dappId,
                        requestId = args.requestId,
                        error = failure.toWalletErrorType(),
                        message = failure.getDappMessage()
                    )
                    sendEvent(TransactionApprovalEvent.NavigateBack)
                    incomingRequestRepository.requestHandled(args.requestId)
                    approvalJob = null
                } else {
                    state = state.copy(isSigning = true)
                    state.manifestData?.let { manifest ->
                        val result = transactionClient.signAndSubmitTransaction(manifest)
                        result.onValue { txId ->
                            // Send confirmation to the dApp that tx was submitted before status polling
                            dAppMessenger.sendTransactionWriteResponseSuccess(
                                dappId = transactionWriteRequest.dappId,
                                requestId = args.requestId,
                                txId = txId
                            )

                            val transactionStatus = transactionClient.pollTransactionStatus(txId)
                            transactionStatus.onValue {
                                state = state.copy(isSigning = false, approved = true)
                                approvalJob = null
                                appEventBus.sendEvent(AppEvent.ApprovedTransaction)
                                sendEvent(TransactionApprovalEvent.NavigateBack)
                                incomingRequestRepository.requestHandled(args.requestId)
                            }
                            transactionStatus.onError {
                                state = state.copy(isSigning = false, error = UiMessage.ErrorMessage(error = it))
                                val exception = it as? DappRequestException
                                if (exception != null) {
                                    dAppMessenger.sendWalletInteractionResponseFailure(
                                        dappId = transactionWriteRequest.dappId,
                                        requestId = args.requestId,
                                        error = exception.failure.toWalletErrorType(),
                                        message = exception.failure.getDappMessage()
                                    )
                                    approvalJob = null
                                    sendEvent(TransactionApprovalEvent.NavigateBack)
                                    incomingRequestRepository.requestHandled(args.requestId)
                                }
                            }
                        }
                        result.onError {
                            state = state.copy(isSigning = false, error = UiMessage.ErrorMessage(error = it))
                            val exception = it as? DappRequestException
                            if (exception != null) {
                                dAppMessenger.sendWalletInteractionResponseFailure(
                                    dappId = transactionWriteRequest.dappId,
                                    requestId = args.requestId,
                                    error = exception.failure.toWalletErrorType(),
                                    message = exception.failure.getDappMessage()
                                )
                                approvalJob = null
                                sendEvent(TransactionApprovalEvent.NavigateBack)
                                incomingRequestRepository.requestHandled(args.requestId)
                            }
                        }
                    }
                }
            }
        }
    }

    fun onBackClick() {
        // TODO display dialog are we sure we want to reject transaction?
        viewModelScope.launch {
            if (approvalJob != null || state.approved) {
                sendEvent(TransactionApprovalEvent.NavigateBack)
            } else {
                dAppMessenger.sendWalletInteractionResponseFailure(
                    dappId = transactionWriteRequest.dappId,
                    requestId = args.requestId,
                    error = WalletErrorType.RejectedByUser
                )
                sendEvent(TransactionApprovalEvent.NavigateBack)
                incomingRequestRepository.requestHandled(args.requestId)
            }
        }
    }

    fun onMessageShown() {
        state = state.copy(error = null)
    }
}

data class TransactionAccountItemUiModel(
    val address: String,
    val displayName: String,
    val tokenSymbol: String,
    val tokenQuantity: String,
    val fiatAmount: String,
    val appearanceID: Int,
    val iconUrl: String
)

data class PresentingProofUiModel(
    val iconUrl: String,
    val title: String
)

data class ConnectedDAppUiModel(
    val icon: String,
    val title: String
)

internal data class TransactionUiState(
    val manifestData: TransactionManifestData? = null,
    val manifestString: String = "",
    val isLoading: Boolean = true,
    val isSigning: Boolean = false,
    val approved: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val error: UiMessage? = null,
    val canApprove: Boolean = false,
    val networkFee: String = "",
    val transactionMessage: String = "",
    val withdrawingAccounts: ImmutableList<TransactionAccountItemUiModel> = persistentListOf(),
    val depositingAccounts: ImmutableList<TransactionAccountItemUiModel> = persistentListOf(),
    val presentingProofs: ImmutableList<PresentingProofUiModel> = persistentListOf(),
    val connectedDApps: ImmutableList<ConnectedDAppUiModel> = persistentListOf()
)

internal sealed interface TransactionApprovalEvent : OneOffEvent {
    object NavigateBack : TransactionApprovalEvent
}
