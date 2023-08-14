package com.babylon.wallet.android.presentation.transaction

import androidx.annotation.FloatRange
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.toPrettyString
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.model.Badge
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesMetadataUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppsUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel.Event
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel.State
import com.babylon.wallet.android.presentation.transaction.analysis.TransactionAnalysisDelegate
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFeesDelegate
import com.babylon.wallet.android.presentation.transaction.guarantees.TransactionGuaranteesDelegate
import com.babylon.wallet.android.presentation.transaction.submit.TransactionSubmitDelegate
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.core.ret.crypto.PrivateKey
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class TransactionApprovalViewModel @Inject constructor(
    private val transactionClient: TransactionClient,
    getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    getResourcesMetadataUseCase: GetResourcesMetadataUseCase,
    getProfileUseCase: GetProfileUseCase,
    getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    resolveDAppsUseCase: ResolveDAppsUseCase,
    getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    dAppMessenger: DappMessenger,
    appEventBus: AppEventBus,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val deviceSecurityHelper: DeviceSecurityHelper,
    @ApplicationScope private val appScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<State>(),
    OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = TransactionApprovalArgs(savedStateHandle)
    private val logger = Timber.tag("TransactionApproval")

    override fun initialState(): State = State(
        request = incomingRequestRepository.getTransactionWriteRequest(args.requestId),
        isLoading = true,
        previewType = PreviewType.None,
        isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
    )

    private val analysis: TransactionAnalysisDelegate = TransactionAnalysisDelegate(
        state = _state,
        getProfileUseCase = getProfileUseCase,
        getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase,
        getTransactionBadgesUseCase = getTransactionBadgesUseCase,
        getResourcesMetadataUseCase = getResourcesMetadataUseCase,
        resolveDAppsUseCase = resolveDAppsUseCase,
        transactionClient = transactionClient,
        logger = logger
    )

    private val guarantees: TransactionGuaranteesDelegate = TransactionGuaranteesDelegate(
        state = _state
    )

    private val fees: TransactionFeesDelegate = TransactionFeesDelegate(
        state = _state,
        getProfileUseCase = getProfileUseCase
    )

    private val submit: TransactionSubmitDelegate = TransactionSubmitDelegate(
        state = _state,
        transactionClient = transactionClient,
        dAppMessenger = dAppMessenger,
        incomingRequestRepository = incomingRequestRepository,
        getCurrentGatewayUseCase = getCurrentGatewayUseCase,
        appScope = appScope,
        appEventBus = appEventBus,
        logger = logger,
        onSendScreenEvent = {
            viewModelScope.launch { sendEvent(it) }
        }
    )

    init {
        viewModelScope.launch {
            transactionClient.signingState.collect { signingState ->
                _state.update { state ->
                    state.copy(interactionState = signingState)
                }
            }
        }

        viewModelScope.launch {
            analysis.analyse()
        }
    }

    fun onBackClick() {
        if (state.value.sheetState != State.Sheet.None) {
            _state.update { it.copy(sheetState = State.Sheet.None) }
        } else {
            viewModelScope.launch {
                submit.onDismiss(DappRequestFailure.RejectedByUser)
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    fun onRawManifestToggle() {
        _state.update { it.copy(isRawManifestVisible = !it.isRawManifestVisible) }
    }

    fun approveTransaction(deviceBiometricAuthenticationProvider: suspend () -> Boolean) {
        submit.onSubmit(deviceBiometricAuthenticationProvider)
    }

    fun promptForGuaranteesClick() = guarantees.onEdit()

    fun onGuaranteeValueChange(account: AccountWithPredictedGuarantee, value: String) = guarantees.onValueChange(
        account = account,
        value = value
    )

    fun onGuaranteeValueIncreased(account: AccountWithPredictedGuarantee) = guarantees.onValueIncreased(account)

    fun onGuaranteeValueDecreased(account: AccountWithPredictedGuarantee) = guarantees.onValueDecreased(account)

    fun onGuaranteesApplyClick() = guarantees.onApply()

    fun onGuaranteesCloseClick() = guarantees.onClose()

    fun onCustomizeClick() {
        viewModelScope.launch {
            fees.onCustomizeClick()
        }
    }

    fun onChangeFeePayerClick() = fees.onChangeFeePayerClick()

    fun onSelectFeePayerClick() = fees.onSelectFeePayerClick()

    fun onNetworkAndRoyaltyFeeChanged(networkAndRoyaltyFee: String) =
        fees.onFeePaddingAmountChanged(networkAndRoyaltyFee)

    fun onTipPercentageChanged(tipPercentage: String) = fees.onTipPercentageChanged(tipPercentage)

    fun onViewDefaultModeClick() = fees.onViewDefaultModeClick()

    fun onViewAdvancedModeClick() = fees.onViewAdvancedModeClick()

    fun onPayerSelected(selectedFeePayer: Network.Account) {
        val feePayerResult = state.value.feePayerSearchResult
        val customizeFeesSheet = state.value.sheetState as? State.Sheet.CustomizeFees ?: return
        _state.update {
            it.copy(
                feePayerSearchResult = feePayerResult?.copy(
                    feePayerAddressFromManifest = selectedFeePayer.address,
                    candidates = feePayerResult.candidates
                ),
                sheetState = customizeFeesSheet.copy(
                    feePayerMode = State.Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                        feePayerCandidate = selectedFeePayer
                    )
                )
            )
        }
    }

    fun onDAppClick(dApp: DAppWithMetadataAndAssociatedResources) {
        _state.update {
            it.copy(sheetState = State.Sheet.Dapp(dApp))
        }
    }

    data class State(
        val request: MessageFromDataChannel.IncomingRequest.TransactionRequest,
        val isDeviceSecure: Boolean,
        val isLoading: Boolean,
        val isSubmitting: Boolean = false,
        val isRawManifestVisible: Boolean = false,
        val previewType: PreviewType,
        val transactionFees: TransactionFees = TransactionFees(),
        val feePayerSearchResult: FeePayerSearchResult? = null,
        val sheetState: Sheet = Sheet.None,
        val error: UiMessage? = null,
        val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom(),
        val interactionState: InteractionState? = null
    ) : UiState {

        val isRawManifestToggleVisible: Boolean
            get() = previewType is PreviewType.Transaction

        val rawManifest: String = request.transactionManifestData.toTransactionManifest().getOrNull()?.toPrettyString().orEmpty()

        val isSheetVisible: Boolean
            get() = sheetState != Sheet.None

        val message: String?
            get() {
                val message = request.transactionManifestData.message
                return if (!message.isNullOrBlank()) {
                    message
                } else {
                    null
                }
            }

        val isSubmitEnabled: Boolean
            get() = previewType !is PreviewType.None

        val noFeePayerSelected: Boolean
            get() = feePayerSearchResult?.feePayerAddressFromManifest == null

        val insufficientBalanceToPayTheFee: Boolean
            get() = false // todo Will need to be added later on

        sealed class Sheet {
            object None : Sheet()

            data class CustomizeGuarantees(
                val accountsWithPredictedGuarantees: List<AccountWithPredictedGuarantee>
            ) : Sheet()

            data class CustomizeFees(
                val feePayerMode: FeePayerMode = FeePayerMode.NoFeePayerRequired,
                val feesMode: FeesMode = FeesMode.Default
            ) : Sheet() {

                sealed interface FeePayerMode {
                    object NoFeePayerRequired : FeePayerMode

                    data class FeePayerSelected(
                        val feePayerCandidate: Network.Account
                    ) : FeePayerMode

                    data class NoFeePayerSelected(
                        val candidates: List<Network.Account>
                    ) : FeePayerMode

                    data class SelectFeePayer(
                        val candidates: List<Network.Account>
                    ) : FeePayerMode
                }

                enum class FeesMode {
                    Default, Advanced
                }
            }

            data class Dapp(
                val dApp: DAppWithMetadataAndAssociatedResources
            ) : Sheet()
        }
    }

    sealed interface Event : OneOffEvent {
        object Dismiss : Event
    }
}

sealed interface PreviewType {
    object None : PreviewType

    object NonConforming : PreviewType

    data class Transaction(
        val from: List<AccountWithTransferableResources>,
        val to: List<AccountWithTransferableResources>,
        val badges: List<Badge> = emptyList(),
        val dApps: List<DAppWithMetadataAndAssociatedResources> = emptyList()
    ) : PreviewType
}

@Suppress("MagicNumber")
sealed interface AccountWithPredictedGuarantee {

    val address: String
    val transferableAmount: TransferableResource.Amount
    val instructionIndex: Long
    val guaranteeAmountString: String

    val guaranteeOffsetDecimal: Float
        @FloatRange(from = 0.0)
        get() = (guaranteeAmountString.toFloatOrNull() ?: 0f) / 100f

    val guaranteedAmount: BigDecimal
        get() = transferableAmount.amount * guaranteeOffsetDecimal.toBigDecimal()

    fun increase(): AccountWithPredictedGuarantee {
        val newOffset = (guaranteeOffsetDecimal.toBigDecimal().plus(BigDecimal(0.001)))
            .multiply(BigDecimal(100)).setScale(1, RoundingMode.HALF_EVEN)
        return when (this) {
            is Other -> copy(guaranteeAmountString = newOffset.toString())
            is Owned -> copy(guaranteeAmountString = newOffset.toString())
        }
    }

    fun decrease(): AccountWithPredictedGuarantee {
        val newOffset = (
            guaranteeOffsetDecimal.toBigDecimal().minus(BigDecimal(0.001))
                .coerceAtLeast(BigDecimal.ZERO).multiply(BigDecimal(100))
            ).setScale(1, RoundingMode.HALF_EVEN)
        return when (this) {
            is Other -> copy(guaranteeAmountString = newOffset.toString())
            is Owned -> copy(guaranteeAmountString = newOffset.toString())
        }
    }

    fun change(amount: String): AccountWithPredictedGuarantee {
        val value = amount.toFloatOrNull() ?: 0f
        return if (value >= 0f) {
            when (this) {
                is Other -> copy(guaranteeAmountString = amount)
                is Owned -> copy(guaranteeAmountString = amount)
            }
        } else {
            this
        }
    }

    data class Owned(
        val account: Network.Account,
        override val transferableAmount: TransferableResource.Amount,
        override val instructionIndex: Long,
        override val guaranteeAmountString: String
    ) : AccountWithPredictedGuarantee {
        override val address: String
            get() = account.address
    }

    data class Other(
        override val address: String,
        override val transferableAmount: TransferableResource.Amount,
        override val instructionIndex: Long,
        override val guaranteeAmountString: String
    ) : AccountWithPredictedGuarantee
}

sealed interface AccountWithTransferableResources {

    val address: String
    val resources: List<Transferable>

    data class Owned(
        val account: Network.Account,
        override val resources: List<Transferable>
    ) : AccountWithTransferableResources {
        override val address: String
            get() = account.address
    }

    data class Other(
        override val address: String,
        override val resources: List<Transferable>
    ) : AccountWithTransferableResources

    fun updateFromGuarantees(
        accountsWithPredictedGuarantees: List<AccountWithPredictedGuarantee>
    ): AccountWithTransferableResources {
        val resourcesWithGuaranteesForAccount = accountsWithPredictedGuarantees.filter {
            it.address == address
        }

        val resources = resources.mapWhen(
            predicate = { depositing ->
                resourcesWithGuaranteesForAccount.any {
                    it.address == address && it.transferableAmount.resourceAddress == depositing.transferable.resourceAddress
                }
            },
            mutation = { depositing ->
                val accountWithGuarantee = resourcesWithGuaranteesForAccount.find {
                    it.transferableAmount.resourceAddress == depositing.transferable.resourceAddress
                }

                if (accountWithGuarantee != null) {
                    depositing.updateGuarantee(accountWithGuarantee.guaranteeOffsetDecimal)
                } else {
                    depositing
                }
            }
        )
        return when (this) {
            is Other -> copy(resources = resources)
            is Owned -> copy(resources = resources)
        }
    }
}

fun List<AccountWithTransferableResources>.hasCustomizableGuarantees() = any { accountWithTransferableResources ->
    accountWithTransferableResources.resources.any { it.guaranteeAssertion is GuaranteeAssertion.ForAmount }
}
