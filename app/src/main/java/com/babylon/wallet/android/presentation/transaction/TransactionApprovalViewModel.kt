package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
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
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.domain.usecases.transaction.GetValidDAppMetadataUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import com.radixdlt.toolkit.models.address.EntityAddress
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.decodeHex
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class TransactionApprovalViewModel @Inject constructor(
    private val transactionClient: TransactionClient,
    private val getTransactionComponentResourcesUseCase: GetTransactionComponentResourcesUseCase,
    private val getTransactionProofResourcesUseCase: GetTransactionProofResourcesUseCase,
    private val getValidDAppMetadataUseCase: GetValidDAppMetadataUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val dAppMessenger: DappMessenger,
    @ApplicationScope private val appScope: CoroutineScope,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<TransactionUiState>(), OneOffEventHandler<TransactionApprovalEvent> by OneOffEventHandlerImpl() {

    private val args = TransactionApprovalArgs(savedStateHandle)
    private val transactionWriteRequest =
        incomingRequestRepository.getTransactionWriteRequest(args.requestId)

    override fun initialState(): TransactionUiState =
        TransactionUiState(isDeviceSecure = deviceSecurityHelper.isDeviceSecure())

    private var approvalJob: Job? = null

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    manifestData = transactionWriteRequest.transactionManifestData,
                    transactionMessage = transactionWriteRequest.transactionManifestData.message.orEmpty()
                )
            }
            val manifestResult = transactionClient.addLockFeeToTransactionManifestData(
                transactionWriteRequest.transactionManifestData
            )

            manifestResult.onValue { manifestWithLockFee ->
                when (
                    val manifestInStringFormatConversionResult =
                        transactionClient.manifestInStringFormat(
                            manifestWithLockFee
                        )
                ) {
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = UiMessage.ErrorMessage(
                                    manifestInStringFormatConversionResult.exception
                                )
                            )
                        }
                    }
                    is Result.Success -> {
                        val transactionManifest = manifestInStringFormatConversionResult.data
                        val transactionPreview = transactionClient.getTransactionPreview(
                            manifest = transactionManifest,
                            networkId = getCurrentGatewayUseCase().network.networkId().value,
                            blobs = transactionManifest.blobs.orEmpty()
                        )
                        transactionPreview.onError { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = UiMessage.ErrorMessage(error)
                                )
                            }
                        }
                        transactionPreview.onValue { transactionPreviewResponse ->

                            if (transactionPreviewResponse.receipt.isFailed) {
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        error = UiMessage.ErrorMessage(
                                            error = Throwable(transactionPreviewResponse.receipt.errorMessage)
                                        )
                                    )
                                }
                            } else {
                                transactionPreviewResponse.receipt.fee_summary.let { feeSummary ->
                                    // TODO this will be done properly when backend work comes
//                                    val costUnitPrice = feeSummary.cost_unit_price.toBigDecimal()
//                                    val costUnitsConsumed = feeSummary.cost_units_consumed.toBigDecimal()
                                    val networkFee = "10" // TODO this will be done properly when backend work comes
                                    _state.update { it.copy(networkFee = networkFee) }
                                }

                                val manifestPreview = transactionClient.analyzeManifest(
                                    networkId = getCurrentGatewayUseCase().network.networkId(),
                                    transactionManifest = transactionManifest,
                                    transactionReceipt = transactionPreviewResponse.encodedReceipt.decodeHex()
                                )

                                manifestPreview.exceptionOrNull().let { error ->
                                    _state.update {
                                        it.copy(
                                            isLoading = false,
                                            error = UiMessage.ErrorMessage(error)
                                        )
                                    }
                                }

                                manifestPreview.getOrNull()
                                    ?.let { analyzeManifestWithPreviewResponse ->

                                        val depositJobs: MutableList<Deferred<Result<TransactionAccountItemUiModel>>> =
                                            mutableListOf()
                                        val withdrawJobs: MutableList<Deferred<Result<TransactionAccountItemUiModel>>> =
                                            mutableListOf()

                                        val componentAddresses = analyzeManifestWithPreviewResponse.encounteredAddresses
                                            .componentAddresses.userApplications
                                            .filterIsInstance<EntityAddress.ComponentAddress>()

                                        val encounteredAddresses = getValidDAppMetadataUseCase.invoke(
                                            componentAddresses
                                        )

                                        analyzeManifestWithPreviewResponse.accountDeposits.forEach {
                                            when (it) {
                                                is AccountDeposit.Estimate -> {
                                                    val amount = when (
                                                        val resSpecifier =
                                                            it.resourceSpecifier
                                                    ) {
                                                        is ResourceSpecifier.Amount -> {
                                                            resSpecifier.amount
                                                        }
                                                        is ResourceSpecifier.Ids -> {
                                                            ""
                                                        }
                                                    }
                                                    val resourceAddress = when (
                                                        val resSpecifier =
                                                            it.resourceSpecifier
                                                    ) {
                                                        is ResourceSpecifier.Amount -> {
                                                            resSpecifier.resourceAddress.address
                                                        }
                                                        is ResourceSpecifier.Ids -> {
                                                            resSpecifier.resourceAddress.address
                                                        }
                                                    }
                                                    depositJobs.add(
                                                        async {
                                                            getTransactionComponentResourcesUseCase.invoke(
                                                                componentAddress = it.componentAddress.address,
                                                                resourceAddress = resourceAddress,
                                                                createdEntities = analyzeManifestWithPreviewResponse
                                                                    .createdEntities,
                                                                amount = amount
                                                            )
                                                        }
                                                    )
                                                }
                                                is AccountDeposit.Exact -> {
                                                    val amount = when (
                                                        val resSpecifier =
                                                            it.resourceSpecifier
                                                    ) {
                                                        is ResourceSpecifier.Amount -> {
                                                            resSpecifier.amount
                                                        }
                                                        is ResourceSpecifier.Ids -> {
                                                            ""
                                                        }
                                                    }
                                                    val resourceAddress = when (
                                                        val resSpecifier =
                                                            it.resourceSpecifier
                                                    ) {
                                                        is ResourceSpecifier.Amount -> {
                                                            resSpecifier.resourceAddress.address
                                                        }
                                                        is ResourceSpecifier.Ids -> {
                                                            resSpecifier.resourceAddress.address
                                                        }
                                                    }
                                                    depositJobs.add(
                                                        async {
                                                            getTransactionComponentResourcesUseCase.invoke(
                                                                componentAddress = it.componentAddress.address,
                                                                resourceAddress = resourceAddress,
                                                                createdEntities = analyzeManifestWithPreviewResponse
                                                                    .createdEntities,
                                                                amount = amount
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        analyzeManifestWithPreviewResponse.accountWithdraws.forEach {
                                            val amount =
                                                when (val resSpecifier = it.resourceSpecifier) {
                                                    is ResourceSpecifier.Amount -> {
                                                        resSpecifier.amount
                                                    }
                                                    is ResourceSpecifier.Ids -> {
                                                        ""
                                                    }
                                                }
                                            val resourceAddress =
                                                when (val resSpecifier = it.resourceSpecifier) {
                                                    is ResourceSpecifier.Amount -> {
                                                        resSpecifier.resourceAddress.address
                                                    }
                                                    is ResourceSpecifier.Ids -> {
                                                        resSpecifier.resourceAddress.address
                                                    }
                                                }
                                            withdrawJobs.add(
                                                async {
                                                    getTransactionComponentResourcesUseCase.invoke(
                                                        componentAddress = it.componentAddress.address,
                                                        resourceAddress = resourceAddress,
                                                        createdEntities = analyzeManifestWithPreviewResponse
                                                            .createdEntities,
                                                        amount = amount
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

                                        val withdrawUniqueAccounts = withdrawingAccounts.distinctBy {
                                            it.address
                                        }

                                        val depositUniqueAccounts = depositingAccounts.distinctBy {
                                            it.address
                                        }

                                        val withdrawPreviewAccounts = withdrawUniqueAccounts.map { uniqueAccount ->
                                            PreviewAccountItemsUiModel(
                                                accountName = uniqueAccount.displayName,
                                                appearanceID = uniqueAccount.appearanceID,
                                                accounts = withdrawingAccounts.filter { it.address == uniqueAccount.address }
                                            )
                                        }.toPersistentList()

                                        val depositPreviewAccounts = depositUniqueAccounts.map { uniqueAccount ->
                                            PreviewAccountItemsUiModel(
                                                accountName = uniqueAccount.displayName,
                                                appearanceID = uniqueAccount.appearanceID,
                                                accounts = depositingAccounts.filter { it.address == uniqueAccount.address }
                                            )
                                        }.toPersistentList()

                                        _state.update {
                                            it.copy(
                                                withdrawingAccounts = withdrawPreviewAccounts,
                                                depositingAccounts = depositPreviewAccounts,
                                                presentingProofs = proofs.toPersistentList(),
                                                connectedDApps = encounteredAddresses.toPersistentList(),
                                                manifestString = manifestInStringFormatConversionResult.data.toPrettyString(),
                                                manifestData = transactionWriteRequest.transactionManifestData,
                                                canApprove = true,
                                                isLoading = false
                                            )
                                        }
                                    }
                            }
                        }
                    }
                }
            }
            manifestResult.onError { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiMessage.ErrorMessage(error)
                    )
                }
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
            _state.value.manifestData?.let { manifestData ->
                val currentNetworkId = getCurrentGatewayUseCase().network.networkId().value
                if (currentNetworkId != manifestData.networkId) {
                    val failure =
                        DappRequestFailure.WrongNetwork(currentNetworkId, manifestData.networkId)
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
                    _state.update { it.copy(isSigning = true) }
                    _state.value.manifestData?.let { manifest ->
                        val result = transactionClient.signAndSubmitTransaction(manifest)
                        result.onValue { txId ->
                            // Send confirmation to the dApp that tx was submitted before status polling
                            dAppMessenger.sendTransactionWriteResponseSuccess(
                                dappId = transactionWriteRequest.dappId,
                                requestId = args.requestId,
                                txId = txId
                            )

                            val transactionStatus = transactionClient.pollTransactionStatus(txId)
                            transactionStatus.onValue { _ ->
                                _state.update { it.copy(isSigning = false, approved = true) }
                                approvalJob = null
                                appEventBus.sendEvent(AppEvent.ApprovedTransaction)
                                sendEvent(TransactionApprovalEvent.NavigateBack)
                                incomingRequestRepository.requestHandled(args.requestId)
                            }
                            transactionStatus.onError { error ->
                                _state.update {
                                    it.copy(
                                        isSigning = false,
                                        error = UiMessage.ErrorMessage(error = error)
                                    )
                                }
                                val exception = error as? DappRequestException
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
                        result.onError { error ->
                            _state.update {
                                it.copy(
                                    isSigning = false,
                                    error = UiMessage.ErrorMessage(error = error)
                                )
                            }
                            val exception = error as? DappRequestException
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
            if (approvalJob != null || _state.value.approved) {
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
        _state.update { it.copy(error = null) }
    }
}

data class TransactionAccountItemUiModel(
    val address: String,
    val displayName: String,
    val tokenSymbol: String,
    val tokenQuantity: String,
    val fiatAmount: String,
    val appearanceID: Int,
    val iconUrl: String,
    val isTokenAmountVisible: Boolean
) {
    val tokenQuantityDecimal: BigDecimal
        get() = if (tokenQuantity.isEmpty()) BigDecimal.ZERO else tokenQuantity.toBigDecimal()
}

data class PresentingProofUiModel(
    val iconUrl: String,
    val title: String
)

data class ConnectedDAppsUiModel(
    val iconUrl: String,
    val title: String
)

data class PreviewAccountItemsUiModel(
    val accountName: String,
    val appearanceID: Int,
    val accounts: List<TransactionAccountItemUiModel>
)

data class TransactionUiState(
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
    val withdrawingAccounts: ImmutableList<PreviewAccountItemsUiModel> = persistentListOf(),
    val depositingAccounts: ImmutableList<PreviewAccountItemsUiModel> = persistentListOf(),
    val presentingProofs: ImmutableList<PresentingProofUiModel> = persistentListOf(),
    val connectedDApps: ImmutableList<ConnectedDAppsUiModel> = persistentListOf()
) : UiState

sealed interface TransactionApprovalEvent : OneOffEvent {
    object NavigateBack : TransactionApprovalEvent
}
