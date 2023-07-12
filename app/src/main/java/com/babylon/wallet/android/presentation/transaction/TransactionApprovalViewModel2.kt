package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.toPrettyString
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.SigningState
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.model.Badge
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
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
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2.Event
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2.State
import com.babylon.wallet.android.presentation.transaction.analysis.TransactionAnalysisDelegate
import com.babylon.wallet.android.presentation.transaction.submit.TransactionSubmitDelegate
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.crypto.PrivateKey
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
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
    getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    dAppMessenger: DappMessenger,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val deviceSecurityHelper: DeviceSecurityHelper,
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

    private val analysis: TransactionAnalysisDelegate = TransactionAnalysisDelegate(
        state = _state,
        getProfileUseCase = getProfileUseCase,
        getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase,
        getTransactionBadgesUseCase = getTransactionBadgesUseCase,
        getDAppWithMetadataAndAssociatedResourcesUseCase = getDAppWithMetadataAndAssociatedResourcesUseCase,
        transactionClient = transactionClient
    )

    private val submit: TransactionSubmitDelegate = TransactionSubmitDelegate(
        state = _state,
        transactionClient = transactionClient,
        dAppMessenger = dAppMessenger,
        incomingRequestRepository = incomingRequestRepository,
        getCurrentGatewayUseCase = getCurrentGatewayUseCase,
        appScope = appScope,
        appEventBus = appEventBus,
        onSendScreenEvent = {
            viewModelScope.launch { sendEvent(it)  }
        }
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
            submit.onDismiss(DappRequestFailure.RejectedByUser)
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    fun onRawManifestToggle() {
        _state.update { it.copy(isRawManifestVisible = !it.isRawManifestVisible) }
    }

    fun approveTransaction() {
        submit.onSubmit()
    }

    fun onGuaranteesApplyClick() {
//        _state.update {
//            it.copy(
//                depositingAccounts = depositingAccounts
//            )
//        }
    }

    fun onGuaranteesCloseClick() {
        // Reset local depositing accounts to initial values
//        depositingAccounts = _state.value.depositingAccounts
//        _state.update {
//            it.copy(
//                guaranteesAccounts = depositingAccounts.toGuaranteesAccountsUiModel()
//            )
//        }
    }

    fun resetBottomSheetMode() {
//        _state.update {
//            it.copy(bottomSheetViewMode = BottomSheetMode.Guarantees)
//        }
    }

    fun onPayerConfirmed() {
//        appScope.launch {
//            val selectedPayer = state.value.feePayerCandidates.first()
//            handleTransactionApprovalForFeePayer(selectedPayer.address, manifestToApprove)
//        }
    }

    fun onPayerSelected(accountItemUiModel: AccountItemUiModel) {
//        _state.update { state ->
//            state.copy(
//                feePayerCandidates = state.feePayerCandidates.map {
//                    it.copy(isSelected = it.address == accountItemUiModel.address)
//                }.toPersistentList()
//            )
//        }
    }

    fun onGuaranteeValueChanged(guaranteePair: Pair<String, GuaranteesAccountItemUiModel>) {
//        val guaranteePercentString = guaranteePair.first.trim()
//        val guaranteePercentBigDecimal = try {
//            guaranteePercentString.toBigDecimal()
//        } catch (e: NumberFormatException) {
//            BigDecimal.ZERO
//        }
//
//        if (guaranteePercentBigDecimal > BigDecimal("100") || guaranteePercentBigDecimal < BigDecimal.ZERO) {
//            return
//        }
//
//        val updatedGuaranteedQuantity = guaranteePercentBigDecimal.divide(BigDecimal("100")).multiply(
//            guaranteePair.second.tokenEstimatedAmount.toBigDecimal().stripTrailingZeros()
//        ).toPlainString()
//
//        val currentDepositingAccounts =
//            if (depositingAccounts.isEmpty()) {
//                _state.value.depositingAccounts
//            } else {
//                depositingAccounts
//            }
//
//        currentDepositingAccounts.map { previewAccountUiModel ->
//            if (previewAccountUiModel.accountAddress == guaranteePair.second.address &&
//                previewAccountUiModel.index == guaranteePair.second.index
//            ) {
//                val fungibleResource = previewAccountUiModel.fungibleResource?.copy(
//                    amount = previewAccountUiModel.fungibleResource.amount,
//                )
//
//                previewAccountUiModel.copy(
//                    accountAddress = previewAccountUiModel.accountAddress,
//                    displayName = previewAccountUiModel.displayName,
//                    appearanceID = previewAccountUiModel.appearanceID,
//                    tokenSymbol = previewAccountUiModel.tokenSymbol,
//                    iconUrl = previewAccountUiModel.iconUrl,
//                    shouldPromptForGuarantees = previewAccountUiModel.shouldPromptForGuarantees,
//                    guaranteedAmount = updatedGuaranteedQuantity,
//                    guaranteedPercentAmount = guaranteePercentString,
//                    instructionIndex = previewAccountUiModel.instructionIndex,
//                    resourceAddress = previewAccountUiModel.resourceAddress,
//                    index = previewAccountUiModel.index,
//                    fungibleResource = fungibleResource,
//                    nonFungibleResourceItems = previewAccountUiModel.nonFungibleResourceItems
//                )
//            } else {
//                previewAccountUiModel
//            }
//        }.toImmutableList().apply {
//            depositingAccounts = this
//            _state.update {
//                it.copy(
//                    guaranteesAccounts = toGuaranteesAccountsUiModel()
//                )
//            }
//        }
    }

    fun promptForGuaranteesClick() {
//        _state.update {
//            it.copy(
//                bottomSheetViewMode = BottomSheetMode.Guarantees
//            )
//        }
    }

    fun onDAppClick(dApp: DAppWithMetadataAndAssociatedResources) {
//        _state.update {
//            it.copy(
//                bottomSheetViewMode = BottomSheetMode.DApp(dApp)
//            )
//        }
    }

    data class State(
        val request: MessageFromDataChannel.IncomingRequest.TransactionRequest,
        val isDeviceSecure: Boolean,
        val isLoading: Boolean,
        val isSubmitting: Boolean = false,
        val isSigning: Boolean = false,
        val isRawManifestVisible: Boolean = false,
        val previewType: PreviewType,
        val fees: TransactionFees = TransactionFees(),
        val error: UiMessage? = null,
        val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom(),
        val networkFee: BigDecimal = TransactionConfig.NETWORK_FEE.toBigDecimal(),
        val signingState: SigningState? = null
    ): UiState {

        val rawManifest: String = request.transactionManifestData.toTransactionManifest().toPrettyString()

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



