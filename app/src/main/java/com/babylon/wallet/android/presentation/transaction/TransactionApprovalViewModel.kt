package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.manifest.addGuaranteeInstructionToManifest
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.SigningState
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.model.TransactionApprovalRequest
import com.babylon.wallet.android.data.transaction.toPrettyString
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.usecases.GetDAppWithMetadataAndAssociatedResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionComponentResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionProofResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.babylon.wallet.android.domain.usecases.transaction.ResourceRequest
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import com.babylon.wallet.android.utils.iconUrl
import com.babylon.wallet.android.utils.toResourceRequest
import com.babylon.wallet.android.utils.tokenSymbol
import com.radixdlt.toolkit.models.crypto.PrivateKey
import com.radixdlt.toolkit.models.request.AccountDeposit
import com.radixdlt.toolkit.models.request.AnalyzeTransactionExecutionResponse
import com.radixdlt.toolkit.models.request.ConvertManifestResponse
import com.radixdlt.toolkit.models.request.ResourceQuantifier
import com.radixdlt.toolkit.models.transaction.ManifestInstructions
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.decodeHex
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber
import java.lang.Exception
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class TransactionApprovalViewModel @Inject constructor(
    private val transactionClient: TransactionClient,
    private val getTransactionComponentResourcesUseCase: GetTransactionComponentResourcesUseCase,
    private val getTransactionProofResourcesUseCase: GetTransactionProofResourcesUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val dAppMessenger: DappMessenger,
    private val dAppWithAssociatedResourcesUseCase: GetDAppWithMetadataAndAssociatedResourcesUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<TransactionUiState>(),
    OneOffEventHandler<TransactionApprovalEvent> by OneOffEventHandlerImpl() {

    private val args = TransactionApprovalArgs(savedStateHandle)
    private val transactionWriteRequest =
        incomingRequestRepository.getTransactionWriteRequest(args.requestId)

    private val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom()

    override fun initialState(): TransactionUiState =
        TransactionUiState(isDeviceSecure = deviceSecurityHelper.isDeviceSecure())

    private var approvalJob: Job? = null

    private var depositingAccounts: ImmutableList<TransactionAccountItemUiModel> = persistentListOf()

    private lateinit var manifestToApprove: TransactionManifest

    init {
        viewModelScope.launch {
            transactionClient.signingState.filterNotNull().collect { signingState ->
                _state.update { state ->
                    state.copy(signingState = signingState)
                }
            }
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    manifestData = transactionWriteRequest.transactionManifestData,
                    transactionMessage = transactionWriteRequest.transactionManifestData.message.orEmpty()
                )
            }
            val manifestInStringFormat = TransactionManifest(
                instructions = ManifestInstructions.StringInstructions(
                    transactionWriteRequest.transactionManifestData.instructions
                ),
                blobs = transactionWriteRequest.transactionManifestData.blobs.toTypedArray()
            )
            val transactionPreview = transactionClient.getTransactionPreview(
                manifest = manifestInStringFormat,
                ephemeralNotaryPrivateKey = ephemeralNotaryPrivateKey,
                blobs = manifestInStringFormat.blobs.orEmpty()
            )
            transactionPreview.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiMessage.ErrorMessage(error)
                    )
                }
            }.onSuccess { transactionPreviewResponse ->
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
                        transactionManifest = manifestInStringFormat,
                        transactionReceipt = transactionPreviewResponse.encodedReceipt.decodeHex()
                    )

                    manifestPreview.exceptionOrNull().let { error ->
                        Timber.e("Analyze manifest failed with error: $error")
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = UiMessage.ErrorMessage(error)
                            )
                        }
                    }

                    manifestPreview.getOrNull()?.let { analyzeManifestWithPreviewResponse ->
                        Timber.d("Manifest : $analyzeManifestWithPreviewResponse")
                        val componentAccountsAddresses = analyzeManifestWithPreviewResponse.encounteredAddresses
                            .componentAddresses.userApplications

                        val encounteredAddressesResults = componentAccountsAddresses.map {
                            dAppWithAssociatedResourcesUseCase.invoke(
                                definitionAddress = it,
                                needMostRecentData = false
                            ).value()
                        }
                        val connectedDApps = encounteredAddressesResults.mapNotNull { dAppWithResources ->
                            dAppWithResources
                        }

                        val accountsWithResources = getTransactionComponentResourcesUseCase.invoke(
                            analyzeManifestWithPreviewResponse
                        ).value().orEmpty()

                        val withdrawTransactionAccountItems = processWithdrawJobs(
                            analyzeManifestWithPreviewResponse,
                            accountsWithResources
                        ).toImmutableList()

                        val depositTransactionAccountItems = processAccountDeposits(
                            analyzeManifestWithPreviewResponse,
                            accountsWithResources
                        ).toImmutableList()

                        val proofs = getTransactionProofResourcesUseCase(
                            analyzeManifestWithPreviewResponse.accountProofResources
                        )

                        val guaranteesAccounts = depositTransactionAccountItems.toGuaranteesAccountsUiModel()

                        depositingAccounts = depositTransactionAccountItems

                        _state.update {
                            it.copy(
                                withdrawingAccounts = withdrawTransactionAccountItems,
                                depositingAccounts = depositTransactionAccountItems,
                                guaranteesAccounts = guaranteesAccounts,
                                presentingProofs = proofs.toPersistentList(),
                                connectedDApps = connectedDApps.toPersistentList(),
                                manifestString = manifestInStringFormat.toPrettyString(),
                                canApprove = true,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun processWithdrawJobs(
        analyzeManifestWithPreviewResponse: AnalyzeTransactionExecutionResponse,
        accountsWithResources: List<AccountWithResources>
    ): List<TransactionAccountItemUiModel> {
        return analyzeManifestWithPreviewResponse.accountWithdraws.map { accountWithdraw ->

            val componentAddress = accountWithdraw.componentAddress

            val accountWithResource = accountsWithResources.find {
                it.account.address == componentAddress
            }

            val resourceRequest = accountWithdraw.resourceQuantifier.toResourceRequest(
                newlyCreated = analyzeManifestWithPreviewResponse.newlyCreated
            )
            val resourceAddress = (resourceRequest as? ResourceRequest.Existing)?.address.orEmpty()

            var fungibleResource: Resource.FungibleResource? = null
            var nonFungibleResourcesItems: List<Resource.NonFungibleResource.Item> = emptyList()

            var amount = ""
            when (val resourceQuantifier = accountWithdraw.resourceQuantifier) {
                is ResourceQuantifier.Amount -> {
                    // fungible
                    amount = resourceQuantifier.amount
                    val fungibleToken = accountWithResource?.resources?.fungibleResources?.find {
                        it.resourceAddress == resourceAddress
                    }
                    fungibleResource = fungibleToken?.copy(
                        amount = amount.toBigDecimal()
                    )
                }
                is ResourceQuantifier.Ids -> {
                    // nonfungible
                    val nonFungibleResource = accountWithResource?.resources?.nonFungibleResources?.find {
                        it.resourceAddress == resourceAddress
                    }
                    nonFungibleResourcesItems = nonFungibleResource?.items?.filter {
                        resourceQuantifier.ids.contains(it.localId.code)
                    }.orEmpty()
                }
            }

            TransactionAccountItemUiModel(
                accountAddress = accountWithResource?.account?.address.orEmpty(),
                displayName = accountWithResource?.account?.displayName.orEmpty(),
                appearanceID = accountWithResource?.account?.appearanceID ?: 0,
                tokenAmount = amount,
                shouldPromptForGuarantees = false,
                guaranteedAmount = null,
                instructionIndex = null,
                resourceAddress = resourceAddress,
                index = null,
                fungibleResource = fungibleResource,
                nonFungibleResourceItems = nonFungibleResourcesItems
            )
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun processAccountDeposits(
        analyzeManifestWithPreviewResponse: AnalyzeTransactionExecutionResponse,
        accountsWithResources: List<AccountWithResources>
    ): List<TransactionAccountItemUiModel> {
        return analyzeManifestWithPreviewResponse.accountDeposits.mapIndexed { index, accountDeposit ->

            when (accountDeposit) {
                is AccountDeposit.Predicted -> {
                    val componentAddress = accountDeposit.componentAddress

                    val accountWithResource = accountsWithResources.find {
                        it.account.address == componentAddress
                    }

                    val resourceRequest = accountDeposit.resourceQuantifier.toResourceRequest(
                        newlyCreated = analyzeManifestWithPreviewResponse.newlyCreated
                    )
                    val createdEntity = resourceRequest is ResourceRequest.NewlyCreated
                    val resourceAddress = (resourceRequest as? ResourceRequest.Existing)?.address.orEmpty()

                    var fungibleResource: Resource.FungibleResource? = null
                    var nonFungibleResourcesItems: List<Resource.NonFungibleResource.Item> = emptyList()

                    var tokenSymbol: String? = null
                    var iconUrl: String? = null
                    var amount = ""
                    if (!createdEntity) {
                        when (accountDeposit.resourceQuantifier) {
                            is ResourceQuantifier.Amount -> {
                                amount = (accountDeposit.resourceQuantifier as ResourceQuantifier.Amount).amount
                                val fungibleToken = accountWithResource?.resources?.fungibleResources?.find {
                                    it.resourceAddress == resourceAddress
                                }
                                fungibleResource = fungibleToken?.copy(
                                    amount = amount.toBigDecimal()
                                )
                            }

                            is ResourceQuantifier.Ids -> {
                                val ids = (accountDeposit.resourceQuantifier as ResourceQuantifier.Ids).ids
                                val nonFungibleResource = accountWithResource?.resources?.nonFungibleResources?.find {
                                    it.resourceAddress == resourceAddress
                                }
                                nonFungibleResourcesItems = nonFungibleResource?.items?.filter {
                                    ids.contains(it.localId.code)
                                }.orEmpty()
                            }
                        }
                    } else {
                        (resourceRequest as ResourceRequest.NewlyCreated).apply {
                            tokenSymbol = this.tokenSymbol()
                            iconUrl = this.iconUrl()
                        }
                        amount = (accountDeposit.resourceQuantifier as ResourceQuantifier.Amount).amount
                    }

                    // If its newlyCreatedEntity do not ask for guarantees
                    val shouldPromptForGuarantees = !createdEntity

                    // If its newlyCreatedEntity OR don't include guarantee, do not ask for guarantees
                    val guaranteedAmount = if (createdEntity) null else amount

                    val instructionIndex = accountDeposit.instructionIndex.toInt()

                    TransactionAccountItemUiModel(
                        accountAddress = accountWithResource?.account?.address.orEmpty(),
                        displayName = accountWithResource?.account?.displayName.orEmpty(),
                        appearanceID = accountWithResource?.account?.appearanceID ?: 0,
                        tokenSymbol = tokenSymbol,
                        tokenAmount = amount,
                        iconUrl = iconUrl,
                        shouldPromptForGuarantees = shouldPromptForGuarantees,
                        guaranteedAmount = guaranteedAmount,
                        instructionIndex = instructionIndex,
                        resourceAddress = resourceAddress,
                        index = index,
                        fungibleResource = fungibleResource,
                        nonFungibleResourceItems = nonFungibleResourcesItems
                    )
                }
                is AccountDeposit.Guaranteed -> {
                    val componentAddress = accountDeposit.componentAddress

                    val accountWithResource = accountsWithResources.find {
                        it.account.address == componentAddress
                    }

                    val resourceRequest = accountDeposit.resourceQuantifier.toResourceRequest(
                        newlyCreated = analyzeManifestWithPreviewResponse.newlyCreated
                    )
                    val createdEntity = resourceRequest is ResourceRequest.NewlyCreated
                    val resourceAddress = (resourceRequest as? ResourceRequest.Existing)?.address.orEmpty()

                    var fungibleResource: Resource.FungibleResource? = null
                    var nonFungibleResourcesItems: List<Resource.NonFungibleResource.Item> = emptyList()

                    var tokenSymbol: String? = null
                    var iconUrl: String? = null
                    var amount = ""
                    if (!createdEntity) {
                        when (accountDeposit.resourceQuantifier) {
                            is ResourceQuantifier.Amount -> {
                                amount = (accountDeposit.resourceQuantifier as ResourceQuantifier.Amount).amount
                                val fungibleToken = accountWithResource?.resources?.fungibleResources?.find {
                                    it.resourceAddress == resourceAddress
                                }
                                fungibleResource = fungibleToken?.copy(
                                    amount = amount.toBigDecimal()
                                )
                            }

                            is ResourceQuantifier.Ids -> {
                                val ids = (accountDeposit.resourceQuantifier as ResourceQuantifier.Ids).ids

                                var nonFungibleResource: Resource.NonFungibleResource? = null
                                accountsWithResources.forEach {
                                    it.resources?.nonFungibleResources?.forEach { nftResource ->
                                        if (nftResource.resourceAddress == resourceAddress) {
                                            nonFungibleResource = nftResource
                                        }
                                    }
                                }
                                nonFungibleResourcesItems = nonFungibleResource?.items?.filter {
                                    ids.contains(it.localId.code)
                                }.orEmpty()
                            }
                        }
                    } else {
                        (resourceRequest as ResourceRequest.NewlyCreated).apply {
                            tokenSymbol = this.tokenSymbol()
                            iconUrl = this.iconUrl()
                        }
                        amount = (accountDeposit.resourceQuantifier as ResourceQuantifier.Amount).amount
                    }

                    TransactionAccountItemUiModel(
                        accountAddress = accountWithResource?.account?.address.orEmpty(),
                        displayName = accountWithResource?.account?.displayName.orEmpty(),
                        appearanceID = accountWithResource?.account?.appearanceID ?: 0,
                        tokenSymbol = tokenSymbol,
                        tokenAmount = amount,
                        iconUrl = iconUrl,
                        shouldPromptForGuarantees = false,
                        guaranteedAmount = null,
                        instructionIndex = null,
                        resourceAddress = resourceAddress,
                        index = null,
                        fungibleResource = fungibleResource,
                        nonFungibleResourceItems = nonFungibleResourcesItems
                    )
                }
            }
        }
    }

    @Suppress("LongMethod")
    fun approveTransaction() {
        approvalJob = appScope.launch {
            _state.value.manifestData?.let { manifestData ->
                val currentNetworkId = getCurrentGatewayUseCase().network.networkId().value
                if (currentNetworkId != manifestData.networkId) {
                    approvalJob = null
                    val failure = DappRequestFailure.WrongNetwork(currentNetworkId, manifestData.networkId)
                    dismissTransaction(failure = failure)
                } else {
                    _state.update { it.copy(isLoading = true) }
                    val txManifest = TransactionManifest(
                        instructions = ManifestInstructions.StringInstructions(manifestData.instructions),
                        blobs = manifestData.blobs.toTypedArray()
                    )
                    transactionClient.convertManifestInstructionsToJSON(
                        txManifest
                    ).onSuccess { manifestJsonResponse ->
                        var manifestJson = manifestJsonResponse
                        _state.value.depositingAccounts.map { transactionAccountUiItem ->
                            transactionAccountUiItem.guaranteedAmount?.let { guaranteedAmount ->
                                manifestJson = manifestJson.addGuaranteeInstructionToManifest(
                                    address = transactionAccountUiItem.resourceAddress.orEmpty(),
                                    guaranteedAmount = guaranteedAmount,
                                    index = transactionAccountUiItem.instructionIndex ?: 0
                                )
                            }
                        }
                        manifestToApprove = manifestJson
                        transactionClient.findFeePayerInManifest(manifestJson).onSuccess { feePayerResult ->
                            _state.update { it.copy(isLoading = false) }
                            if (feePayerResult.feePayerAddressFromManifest != null) {
                                handleTransactionApprovalForFeePayer(feePayerResult.feePayerAddressFromManifest, manifestJson)
                            } else {
                                _state.update { state ->
                                    state.copy(
                                        feePayerCandidates = feePayerResult.candidates.map { it.toUiModel() }.toPersistentList(),
                                        bottomSheetViewMode = BottomSheetMode.FeePayerSelection
                                    )
                                }
                                sendEvent(TransactionApprovalEvent.SelectFeePayer)
                            }
                            approvalJob = null
                        }.onFailure { t ->
                            _state.update { it.copy(isLoading = false, error = UiMessage.ErrorMessage(error = t)) }
                            (t as? DappRequestException)?.let { exception ->
                                if (!transactionWriteRequest.isInternal) {
                                    dAppMessenger.sendWalletInteractionResponseFailure(
                                        dappId = transactionWriteRequest.dappId,
                                        requestId = args.requestId,
                                        error = exception.failure.toWalletErrorType(),
                                        message = exception.failure.getDappMessage()
                                    )
                                }
                            }
                            approvalJob = null
                        }
                    }
                }
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun handleTransactionApprovalForFeePayer(
        feePayerAddress: String,
        manifestJson: ConvertManifestResponse
    ) {
        _state.update { it.copy(isSigning = true) }
        val request = TransactionApprovalRequest(
            manifestJson,
            ephemeralNotaryPrivateKey = ephemeralNotaryPrivateKey,
            feePayerAddress = feePayerAddress
        )
        transactionClient.signAndSubmitTransaction(request).onSuccess { txId ->
            _state.update { it.copy(isSigning = false) }
            appEventBus.sendEvent(
                AppEvent.TransactionEvent.Sent(
                    requestId = args.requestId,
                    transactionId = txId,
                    isInternal = transactionWriteRequest.isInternal
                )
            )
            // Send confirmation to the dApp that tx was submitted before status polling
            if (!transactionWriteRequest.isInternal) {
                dAppMessenger.sendTransactionWriteResponseSuccess(
                    dappId = transactionWriteRequest.dappId,
                    requestId = args.requestId,
                    txId = txId
                )
            }

            appEventBus.sendEvent(
                AppEvent.Status.Transaction.InProgress(
                    requestId = args.requestId,
                    transactionId = txId,
                    isInternal = transactionWriteRequest.isInternal
                )
            )
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isSigning = false,
                    error = UiMessage.ErrorMessage(error = error)
                )
            }
            val exception = error as? DappRequestException
            if (exception != null) {
                if (!transactionWriteRequest.isInternal) {
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        dappId = transactionWriteRequest.dappId,
                        requestId = args.requestId,
                        error = exception.failure.toWalletErrorType(),
                        message = exception.failure.getDappMessage()
                    )
                }
            }

            appEventBus.sendEvent(
                AppEvent.Status.Transaction.Fail(
                    requestId = args.requestId,
                    transactionId = "",
                    isInternal = transactionWriteRequest.isInternal,
                    errorMessageRes = UiMessage.ErrorMessage(exception?.failure).getUserFriendlyDescriptionRes()
                )
            )
        }

        approvalJob = null
    }

    fun onBackClick() {
        viewModelScope.launch {
            dismissTransaction(DappRequestFailure.RejectedByUser)
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

    fun resetBottomSheetMode() {
        _state.update {
            it.copy(bottomSheetViewMode = BottomSheetMode.Guarantees)
        }
    }

    fun onPayerConfirmed() {
        appScope.launch {
            val selectedPayer = state.value.feePayerCandidates.first()
            handleTransactionApprovalForFeePayer(selectedPayer.address, manifestToApprove)
        }
    }

    fun onPayerSelected(accountItemUiModel: AccountItemUiModel) {
        _state.update { state ->
            state.copy(
                feePayerCandidates = state.feePayerCandidates.map {
                    it.copy(isSelected = it.address == accountItemUiModel.address)
                }.toPersistentList()
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
            guaranteePair.second.tokenEstimatedAmount.toBigDecimal().stripTrailingZeros()
        ).toPlainString()

        val currentDepositingAccounts =
            if (depositingAccounts.isEmpty()) {
                _state.value.depositingAccounts
            } else {
                depositingAccounts
            }

        currentDepositingAccounts.map { previewAccountUiModel ->
            if (previewAccountUiModel.accountAddress == guaranteePair.second.address &&
                previewAccountUiModel.index == guaranteePair.second.index
            ) {
                val fungibleResource = previewAccountUiModel.fungibleResource?.copy(
                    amount = previewAccountUiModel.fungibleResource.amount,
                )

                previewAccountUiModel.copy(
                    accountAddress = previewAccountUiModel.accountAddress,
                    displayName = previewAccountUiModel.displayName,
                    appearanceID = previewAccountUiModel.appearanceID,
                    tokenSymbol = previewAccountUiModel.tokenSymbol,
                    iconUrl = previewAccountUiModel.iconUrl,
                    shouldPromptForGuarantees = previewAccountUiModel.shouldPromptForGuarantees,
                    guaranteedAmount = updatedGuaranteedQuantity,
                    guaranteedPercentAmount = guaranteePercentString,
                    instructionIndex = previewAccountUiModel.instructionIndex,
                    resourceAddress = previewAccountUiModel.resourceAddress,
                    index = previewAccountUiModel.index,
                    fungibleResource = fungibleResource,
                    nonFungibleResourceItems = previewAccountUiModel.nonFungibleResourceItems
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

    fun promptForGuaranteesClick() {
        _state.update {
            it.copy(
                bottomSheetViewMode = BottomSheetMode.Guarantees
            )
        }
    }

    fun onDAppClick(dApp: DAppWithMetadataAndAssociatedResources) {
        _state.update {
            it.copy(
                bottomSheetViewMode = BottomSheetMode.DApp(dApp)
            )
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun dismissTransaction(failure: DappRequestFailure) {
        if (approvalJob == null) {
            if (!transactionWriteRequest.isInternal) {
                dAppMessenger.sendWalletInteractionResponseFailure(
                    dappId = transactionWriteRequest.dappId,
                    requestId = args.requestId,
                    error = failure.toWalletErrorType(),
                    message = failure.getDappMessage()
                )
            }
            sendEvent(TransactionApprovalEvent.Dismiss)
            incomingRequestRepository.requestHandled(args.requestId)
        } else {
            Timber.d("Cannot dismiss transaction while is in progress")
        }
    }
}

data class TransactionAccountItemUiModel(
    val accountAddress: String,
    val displayName: String,
    val appearanceID: Int,
    val tokenSymbol: String? = null,
    val tokenAmount: String,
    val iconUrl: String? = null,
    val shouldPromptForGuarantees: Boolean,
    val guaranteedAmount: String?,
    val guaranteedPercentAmount: String = "100",
    val instructionIndex: Int? = null, // Index that instruction will be inserted at in the manifest
    val resourceAddress: String? = null,
    val index: Int? = null, // Unique to identify which item state we are changing as addresses might be the same for,
    val fungibleResource: Resource.FungibleResource? = null,
    val nonFungibleResourceItems: List<Resource.NonFungibleResource.Item> = emptyList()
)

data class GuaranteesAccountItemUiModel(
    val address: String,
    val appearanceID: Int,
    val displayName: String,
    val tokenSymbol: String,
    val tokenIconUrl: String,
    val tokenEstimatedAmount: String,
    val tokenGuaranteedAmount: String,
    val guaranteedPercentAmount: String,
    val index: Int? = null
) {
    fun isXrd(): Boolean = tokenSymbol == MetadataConstants.SYMBOL_XRD
}

data class PresentingProofUiModel(
    val iconUrl: String,
    val title: String
)

fun List<TransactionAccountItemUiModel>.toGuaranteesAccountsUiModel(): ImmutableList<GuaranteesAccountItemUiModel> {
    return filter { it.shouldPromptForGuarantees }
        .map { transactionAccountItemUiModel ->
            val fungibleItem = transactionAccountItemUiModel.fungibleResource
            fungibleItem?.let { item ->
                GuaranteesAccountItemUiModel(
                    address = transactionAccountItemUiModel.accountAddress,
                    appearanceID = transactionAccountItemUiModel.appearanceID,
                    displayName = transactionAccountItemUiModel.displayName,
                    tokenSymbol = item.displayTitle,
                    tokenIconUrl = item.iconUrl.toString(),
                    tokenEstimatedAmount = item.amount?.toPlainString().orEmpty(),
                    tokenGuaranteedAmount = transactionAccountItemUiModel.guaranteedAmount.orEmpty(),
                    guaranteedPercentAmount = transactionAccountItemUiModel.guaranteedPercentAmount,
                    index = transactionAccountItemUiModel.index
                )
            } ?: run {
                GuaranteesAccountItemUiModel(
                    address = transactionAccountItemUiModel.accountAddress,
                    appearanceID = transactionAccountItemUiModel.appearanceID,
                    displayName = transactionAccountItemUiModel.displayName,
                    tokenSymbol = transactionAccountItemUiModel.tokenSymbol.orEmpty(),
                    tokenIconUrl = transactionAccountItemUiModel.iconUrl.orEmpty(),
                    tokenEstimatedAmount = transactionAccountItemUiModel.tokenAmount,
                    tokenGuaranteedAmount = transactionAccountItemUiModel.guaranteedAmount.orEmpty(),
                    guaranteedPercentAmount = transactionAccountItemUiModel.guaranteedPercentAmount,
                    index = transactionAccountItemUiModel.index
                )
            }
        }.toPersistentList()
}

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
    val withdrawingAccounts: ImmutableList<TransactionAccountItemUiModel> = persistentListOf(),
    val depositingAccounts: ImmutableList<TransactionAccountItemUiModel> = persistentListOf(),
    val guaranteesAccounts: ImmutableList<GuaranteesAccountItemUiModel> = persistentListOf(),
    val presentingProofs: ImmutableList<PresentingProofUiModel> = persistentListOf(),
    val connectedDApps: ImmutableList<DAppWithMetadataAndAssociatedResources> = persistentListOf(),
    val bottomSheetViewMode: BottomSheetMode = BottomSheetMode.Guarantees,
    val feePayerCandidates: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val bottomSheetMode: BottomSheetMode = BottomSheetMode.Guarantees,
    val signingState: SigningState? = null
) : UiState {

    val shouldPromptForGuarantees: Boolean
        get() = depositingAccounts.any { it.shouldPromptForGuarantees }

    val depositingAccountsMap: ImmutableMap<String, List<TransactionAccountItemUiModel>>
        get() = depositingAccounts.groupBy {
            it.accountAddress
        }.toPersistentMap()

    val withdrawingAccountsMap: ImmutableMap<String, List<TransactionAccountItemUiModel>>
        get() = withdrawingAccounts.groupBy {
            it.accountAddress
        }.toPersistentMap()
}

sealed interface TransactionApprovalEvent : OneOffEvent {
    object Dismiss : TransactionApprovalEvent
    object SelectFeePayer : TransactionApprovalEvent
}

sealed interface BottomSheetMode {
    object Guarantees : BottomSheetMode
    object FeePayerSelection : BottomSheetMode
    data class DApp(
        val dApp: DAppWithMetadataAndAssociatedResources
    ) : BottomSheetMode
}
