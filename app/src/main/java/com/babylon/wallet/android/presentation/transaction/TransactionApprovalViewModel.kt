package com.babylon.wallet.android.presentation.transaction

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.manifest.addGuaranteeInstructionToManifest
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.readInstructions
import com.babylon.wallet.android.data.transaction.toPrettyString
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionComponentResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionProofResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetValidDAppMetadataUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import com.radixdlt.toolkit.models.address.EntityAddress
import com.radixdlt.toolkit.models.request.AccountDeposit
import com.radixdlt.toolkit.models.request.ResourceSpecifier
import com.radixdlt.toolkit.models.transaction.ManifestInstructions
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
import timber.log.Timber
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

    private var depositingAccounts: ImmutableList<PreviewAccountItemsUiModel> = persistentListOf()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    manifestData = transactionWriteRequest.transactionManifestData,
                    transactionMessage = transactionWriteRequest.transactionManifestData.message.orEmpty()
                )
            }
            val manifestResult = transactionClient.convertManifestInstructionsToJSON(
                manifest = TransactionManifest(
                    instructions = ManifestInstructions.StringInstructions(
                        transactionWriteRequest.transactionManifestData.instructions
                    ),
                    blobs = transactionWriteRequest.transactionManifestData.blobs.toTypedArray()
                )
            )

            manifestResult.onValue { manifestJson ->
                when (
                    val manifestInStringFormatConversionResult = transactionClient.manifestInStringFormat(
                        manifest = manifestJson
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
                                transactionPreviewResponse.receipt.feeSummary.let {
                                    // TODO this will be done properly when backend work comes
//                                    val costUnitPrice = feeSummary.cost_unit_price.toBigDecimal()
//                                    val costUnitsConsumed = feeSummary.cost_units_consumed.toBigDecimal()
                                    val networkFee = "10" // TODO this will be done properly when backend work comes
                                    _state.update { state -> state.copy(networkFee = networkFee) }
                                }

                                val manifestPreview = transactionClient.analyzeManifestWithPreviewContext(
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

                                manifestPreview.getOrNull()?.let { analyzeManifestWithPreviewResponse ->

                                    Timber.d("Manifest : $analyzeManifestWithPreviewResponse")
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

                                    analyzeManifestWithPreviewResponse
                                        .accountDeposits.forEachIndexed { index, accountDeposit ->
                                            when (accountDeposit) {
                                                is AccountDeposit.Estimate -> {
                                                    val amount = when (
                                                        val resSpecifier =
                                                            accountDeposit.resourceSpecifier
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
                                                            accountDeposit.resourceSpecifier
                                                    ) {
                                                        is ResourceSpecifier.Amount -> {
                                                            resSpecifier.resourceAddress.address
                                                        }
                                                        is ResourceSpecifier.Ids -> {
                                                            resSpecifier.resourceAddress.address
                                                        }
                                                    }

                                                    val instructionIndex = accountDeposit.instructionIndex.toInt()

                                                    depositJobs.add(
                                                        async {
                                                            getTransactionComponentResourcesUseCase.invoke(
                                                                componentAddress = accountDeposit
                                                                    .componentAddress.address,
                                                                resourceAddress = resourceAddress,
                                                                createdEntities = analyzeManifestWithPreviewResponse
                                                                    .createdEntities,
                                                                amount = amount,
                                                                instructionIndex = instructionIndex,
                                                                includesGuarantees = true,
                                                                index = index
                                                            )
                                                        }
                                                    )
                                                }
                                                is AccountDeposit.Exact -> {
                                                    val amount = when (
                                                        val resSpecifier =
                                                            accountDeposit.resourceSpecifier
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
                                                            accountDeposit.resourceSpecifier
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
                                                                componentAddress = accountDeposit
                                                                    .componentAddress.address,
                                                                resourceAddress = resourceAddress,
                                                                createdEntities = analyzeManifestWithPreviewResponse
                                                                    .createdEntities,
                                                                amount = amount,
                                                                includesGuarantees = false
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    analyzeManifestWithPreviewResponse.accountWithdraws.forEach { accountWithdraw ->
                                        val amount =
                                            when (val resSpecifier = accountWithdraw.resourceSpecifier) {
                                                is ResourceSpecifier.Amount -> {
                                                    resSpecifier.amount
                                                }
                                                is ResourceSpecifier.Ids -> {
                                                    ""
                                                }
                                            }
                                        val resourceAddress =
                                            when (val resSpecifier = accountWithdraw.resourceSpecifier) {
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
                                                    componentAddress = accountWithdraw.componentAddress.address,
                                                    resourceAddress = resourceAddress,
                                                    createdEntities = analyzeManifestWithPreviewResponse
                                                        .createdEntities,
                                                    amount = amount,
                                                    includesGuarantees = false
                                                )
                                            }
                                        )
                                    }

                                    val depositResults = depositJobs.awaitAll()
                                    val withdrawResults = withdrawJobs.awaitAll()

                                    val withdrawingAccountsResults = withdrawResults
                                        .filterIsInstance<Result.Success<TransactionAccountItemUiModel>>()
                                        .map {
                                            it.data
                                        }

                                    val depositingAccountsResults = depositResults
                                        .filterIsInstance<Result.Success<TransactionAccountItemUiModel>>()
                                        .map {
                                            it.data
                                        }

                                    val proofs = getTransactionProofResourcesUseCase(
                                        analyzeManifestWithPreviewResponse.accountProofResources
                                    )

                                    val withdrawUniqueAccounts = withdrawingAccountsResults.distinctBy {
                                        it.address
                                    }

                                    val depositUniqueAccounts = depositingAccountsResults.distinctBy {
                                        it.address
                                    }

                                    val withdrawPreviewAccounts = withdrawUniqueAccounts.map { uniqueAccount ->
                                        PreviewAccountItemsUiModel(
                                            address = uniqueAccount.address,
                                            accountName = uniqueAccount.displayName,
                                            appearanceID = uniqueAccount.appearanceID,
                                            accounts = withdrawingAccountsResults.filter { it.address == uniqueAccount.address }
                                        )
                                    }.toPersistentList()

                                    val depositPreviewAccounts = depositUniqueAccounts.map { uniqueAccount ->
                                        PreviewAccountItemsUiModel(
                                            address = uniqueAccount.address,
                                            accountName = uniqueAccount.displayName,
                                            appearanceID = uniqueAccount.appearanceID,
                                            accounts = depositingAccountsResults.filter { it.address == uniqueAccount.address }
                                        )
                                    }.toPersistentList()

                                    val guaranteesAccounts = depositPreviewAccounts.toGuaranteesAccountsUiModel()

                                    depositingAccounts = depositPreviewAccounts

                                    _state.update {
                                        it.copy(
                                            withdrawingAccounts = withdrawPreviewAccounts,
                                            depositingAccounts = depositPreviewAccounts,
                                            guaranteesAccounts = guaranteesAccounts,
                                            presentingProofs = proofs.toPersistentList(),
                                            connectedDApps = encounteredAddresses.toPersistentList(),
                                            manifestString = manifestInStringFormatConversionResult.data.toPrettyString(),
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
                    _state.update { it.copy(isSigning = true) }

                    val txManifest = TransactionManifest(
                        instructions = ManifestInstructions.StringInstructions(manifestData.instructions),
                        blobs = manifestData.blobs.toTypedArray()
                    )

                    transactionClient.convertManifestInstructionsToJSON(
                        txManifest
                    ).onValue { manifestJsonResponse ->
                        var manifestJson = manifestJsonResponse
                        _state.value.depositingAccounts.map { previewAccountItemsUiModel ->
                            previewAccountItemsUiModel.accounts.map { accountItemUiModel ->
                                accountItemUiModel.guaranteedQuantity?.let { guaranteedAmount ->
                                    manifestJson = manifestJson.addGuaranteeInstructionToManifest(
                                        address = accountItemUiModel.resourceAddress.orEmpty(),
                                        guaranteedAmount = guaranteedAmount,
                                        index = accountItemUiModel.instructionIndex ?: 0
                                    )
                                }
                            }
                        }

                        transactionClient.convertManifestInstructionsToString(
                            manifestJson
                        ).onValue { manifestStringResponse ->
                            val signAndSubmitResult = transactionClient.signAndSubmitTransaction(
                                instructions = manifestStringResponse.readInstructions(),
                                blobs = manifestStringResponse.blobs
                            )
                            signAndSubmitResult.onValue { txId ->
                                // Send confirmation to the dApp that tx was submitted before status polling
                                dAppMessenger.sendTransactionWriteResponseSuccess(
                                    dappId = transactionWriteRequest.dappId,
                                    requestId = args.requestId,
                                    txId = txId
                                )

                                val transactionStatus = transactionClient.pollTransactionStatus(txId)
                                transactionStatus.onValue { _ ->
                                    _state.update { it.copy(isSigning = false) }
                                    approvalJob = null
                                    appEventBus.sendEvent(AppEvent.ApprovedTransaction)
                                    sendEvent(TransactionApprovalEvent.FlowCompletedWithSuccess(requestId = args.requestId))
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
                                        sendEvent(
                                            TransactionApprovalEvent.FlowCompletedWithError(
                                                requestId = args.requestId,
                                                errorTextRes = exception.failure.toDescriptionRes()
                                            )
                                        )
                                    }
                                }
                            }
                            signAndSubmitResult.onError { error ->
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
                                    sendEvent(
                                        TransactionApprovalEvent.FlowCompletedWithError(
                                            requestId = args.requestId,
                                            errorTextRes = exception.failure.toDescriptionRes()
                                        )
                                    )
                                }
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
            if (approvalJob != null) {
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

    fun onGuaranteesApplyClick() {
        _state.update {
            it.copy(
                depositingAccounts = depositingAccounts
            )
        }
    }

    fun onGuaranteesCloseClick() {
        // Reset local depositing accounts to initial values
        depositingAccounts = _state.value.depositingAccounts
        _state.update {
            it.copy(
                guaranteesAccounts = depositingAccounts.toGuaranteesAccountsUiModel()
            )
        }
    }

    fun onGuaranteeValueChanged(guaranteePair: Pair<String, GuaranteesAccountItemUiModel>) {
        val guaranteePercentString = guaranteePair.first.trim()
        val guaranteePercentBigDecimal = try {
            guaranteePercentString.toBigDecimal()
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }

        if (guaranteePercentBigDecimal > BigDecimal("100") || guaranteePercentBigDecimal < BigDecimal.ZERO) {
            return
        }

        val updatedGuaranteedQuantity = guaranteePercentBigDecimal.divide(BigDecimal("100")).multiply(
            guaranteePair.second.tokenEstimatedQuantity.toBigDecimal().stripTrailingZeros()
        ).toPlainString()

        val currentDepositingAccounts =
            if (depositingAccounts.isEmpty()) {
                _state.value.depositingAccounts
            } else {
                depositingAccounts
            }

        currentDepositingAccounts.map { previewAccountUiModel ->
            if (previewAccountUiModel.address == guaranteePair.second.address) {
                previewAccountUiModel.copy(
                    address = previewAccountUiModel.address,
                    accountName = previewAccountUiModel.accountName,
                    appearanceID = previewAccountUiModel.appearanceID,
                    accounts = previewAccountUiModel.accounts.map { tokenUiModel ->
                        if (tokenUiModel.index == guaranteePair.second.index) {
                            tokenUiModel.copy(
                                address = tokenUiModel.address,
                                displayName = tokenUiModel.displayName,
                                tokenSymbol = tokenUiModel.tokenSymbol,
                                tokenQuantity = tokenUiModel.tokenQuantity,
                                appearanceID = tokenUiModel.appearanceID,
                                iconUrl = tokenUiModel.iconUrl,
                                isTokenAmountVisible = tokenUiModel.isTokenAmountVisible,
                                shouldPromptForGuarantees = tokenUiModel.shouldPromptForGuarantees,
                                guaranteedQuantity = updatedGuaranteedQuantity,
                                guaranteedPercentAmount = guaranteePercentString,
                            )
                        } else {
                            tokenUiModel
                        }
                    }
                )
            } else {
                previewAccountUiModel
            }
        }.toImmutableList().apply {
            depositingAccounts = this
            _state.update {
                it.copy(
                    guaranteesAccounts = toGuaranteesAccountsUiModel()
                )
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
    val appearanceID: Int,
    val iconUrl: String,
    val isTokenAmountVisible: Boolean,
    val shouldPromptForGuarantees: Boolean,
    val guaranteedQuantity: String?,
    val guaranteedPercentAmount: String = "100",
    val instructionIndex: Int? = null, // Index that instruction will be inserted at in the manifest
    val resourceAddress: String? = null,
    val index: Int? = null // Unique to identify which item state we are changing as addresses might be the same for
    // nested elements using the same address
) {
    val tokenQuantityDecimal: BigDecimal
        get() = if (tokenQuantity.isEmpty()) BigDecimal.ZERO else tokenQuantity.toBigDecimal().stripTrailingZeros()

    val guaranteedQuantityDecimal: BigDecimal?
        get() = if (guaranteedQuantity.isNullOrEmpty()) null else guaranteedQuantity.toBigDecimal().stripTrailingZeros()
}

data class GuaranteesAccountItemUiModel(
    val address: String,
    val appearanceID: Int,
    val displayName: String,
    val tokenSymbol: String,
    val tokenIconUrl: String,
    val tokenEstimatedQuantity: String,
    val tokenGuaranteedQuantity: String,
    val guaranteedPercentAmount: String,
    val index: Int? = null
) {
    fun isXrd(): Boolean = tokenSymbol == MetadataConstants.SYMBOL_XRD
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
    val address: String,
    val accountName: String,
    val appearanceID: Int,
    val accounts: List<TransactionAccountItemUiModel>
)

fun List<PreviewAccountItemsUiModel>.toGuaranteesAccountsUiModel() = map { previewAccountItemsUiModel ->
    previewAccountItemsUiModel.accounts
        .filter { it.shouldPromptForGuarantees }
        .map { account ->
            GuaranteesAccountItemUiModel(
                address = account.address,
                appearanceID = previewAccountItemsUiModel.appearanceID,
                displayName = account.displayName,
                tokenSymbol = account.tokenSymbol,
                tokenIconUrl = account.iconUrl,
                tokenEstimatedQuantity = account.tokenQuantity,
                tokenGuaranteedQuantity = account.guaranteedQuantity.orEmpty(),
                guaranteedPercentAmount = account.guaranteedPercentAmount,
                index = account.index
            )
        }
}.flatten().toPersistentList()

data class TransactionUiState(
    val manifestData: TransactionManifestData? = null,
    val manifestString: String = "",
    val isLoading: Boolean = true,
    val isSigning: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val error: UiMessage? = null,
    val canApprove: Boolean = false,
    val networkFee: String = "",
    val transactionMessage: String = "",
    val withdrawingAccounts: ImmutableList<PreviewAccountItemsUiModel> = persistentListOf(),
    val depositingAccounts: ImmutableList<PreviewAccountItemsUiModel> = persistentListOf(),
    val guaranteesAccounts: ImmutableList<GuaranteesAccountItemUiModel> = persistentListOf(),
    val presentingProofs: ImmutableList<PresentingProofUiModel> = persistentListOf(),
    val connectedDApps: ImmutableList<ConnectedDAppsUiModel> = persistentListOf(),
    val guaranteePercent: BigDecimal = BigDecimal("100")
) : UiState

sealed interface TransactionApprovalEvent : OneOffEvent {
    object NavigateBack : TransactionApprovalEvent
    data class FlowCompletedWithSuccess(val requestId: String) : TransactionApprovalEvent
    data class FlowCompletedWithError(
        val requestId: String,
        @StringRes val errorTextRes: Int
    ) : TransactionApprovalEvent
}
