package com.babylon.wallet.android.presentation.transaction

import androidx.annotation.FloatRange
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.toPrettyString
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.model.Badge
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resource.FungibleResource
import com.babylon.wallet.android.domain.model.Resource.NonFungibleResource
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.isXrd
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesMetadataUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppsUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.Event
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.transaction.analysis.TransactionAnalysisDelegate
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFeesDelegate
import com.babylon.wallet.android.presentation.transaction.guarantees.TransactionGuaranteesDelegate
import com.babylon.wallet.android.presentation.transaction.submit.TransactionSubmitDelegate
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.ret.AccountDefaultDepositRule
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
class TransactionReviewViewModel @Inject constructor(
    private val transactionClient: TransactionClient,
    getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    getResourcesMetadataUseCase: GetResourcesMetadataUseCase,
    getProfileUseCase: GetProfileUseCase,
    getResourcesUseCase: GetResourcesUseCase,
    getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    resolveDAppsUseCase: ResolveDAppsUseCase,
    getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    dAppMessenger: DappMessenger,
    appEventBus: AppEventBus,
    transactionStatusClient: TransactionStatusClient,
    incomingRequestRepository: IncomingRequestRepository,
    @ApplicationScope private val appScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<State>(),
    OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = TransactionReviewArgs(savedStateHandle)
    private val logger = Timber.tag("TransactionApproval")

    override fun initialState(): State = State(
        isLoading = true,
        previewType = PreviewType.None
    )

    private val analysis: TransactionAnalysisDelegate = TransactionAnalysisDelegate(
        state = _state,
        getProfileUseCase = getProfileUseCase,
        getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase,
        getTransactionBadgesUseCase = getTransactionBadgesUseCase,
        getResourcesMetadataUseCase = getResourcesMetadataUseCase,
        resolveDAppsUseCase = resolveDAppsUseCase,
        transactionClient = transactionClient,
        logger = logger,
        getResourcesUseCase = getResourcesUseCase
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
        transactionStatusClient = transactionStatusClient,
        onSendScreenEvent = {
            viewModelScope.launch { sendEvent(it) }
        }
    )

    init {
        val request = incomingRequestRepository.getTransactionWriteRequest(args.requestId)
        if (request == null) {
            viewModelScope.launch { sendEvent(Event.Dismiss) }
        } else {
            _state.update { it.copy(request = request) }
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

    fun onCancelSigningClick() {
        transactionClient.cancelSigning()
    }

    fun onChangeFeePayerClick() = fees.onChangeFeePayerClick()

    fun onSelectFeePayerClick() = fees.onSelectFeePayerClick()

    fun onFeePaddingAmountChanged(feePaddingAmount: String) {
        fees.onFeePaddingAmountChanged(feePaddingAmount)
    }

    fun onTipPercentageChanged(tipPercentage: String) {
        fees.onTipPercentageChanged(tipPercentage)
    }

    fun onViewDefaultModeClick() = fees.onViewDefaultModeClick()

    fun onViewAdvancedModeClick() = fees.onViewAdvancedModeClick()

    fun onPayerSelected(selectedFeePayer: Network.Account) {
        val feePayerSearchResult = state.value.feePayerSearchResult
        val transactionFees = state.value.transactionFees
        val signersCount = state.value.defaultSignersCount

        val updatedFeePayerResult = feePayerSearchResult?.copy(
            feePayerAddress = selectedFeePayer.address,
            candidates = feePayerSearchResult.candidates
        )

        val customizeFeesSheet = state.value.sheetState as? State.Sheet.CustomizeFees ?: return

        val selectedFeePayerInvolvedInTransaction = state.value.request?.transactionManifestData?.toTransactionManifest()
            ?.getOrNull()?.let {
                it.accountsWithdrawnFrom() + it.accountsDepositedInto() + it.accountsRequiringAuth()
            }.orEmpty().any { it.addressString() == selectedFeePayer.address }

        val updatedSignersCount = if (selectedFeePayerInvolvedInTransaction) signersCount else signersCount + 1

        _state.update {
            it.copy(
                transactionFees = transactionFees.copy(
                    signersCount = updatedSignersCount
                ),
                feePayerSearchResult = updatedFeePayerResult,
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

    fun onFungibleResourceClick(fungibleResource: FungibleResource) {
        _state.update {
            it.copy(sheetState = State.Sheet.ResourceSelected.Fungible(fungibleResource))
        }
    }

    fun onNonFungibleResourceClick(
        nonFungibleResource: NonFungibleResource,
        item: NonFungibleResource.Item
    ) {
        _state.update {
            it.copy(sheetState = State.Sheet.ResourceSelected.NonFungible(nonFungibleResource, item))
        }
    }

    data class State(
        val request: MessageFromDataChannel.IncomingRequest.TransactionRequest? = null,
        val isLoading: Boolean,
        val isSubmitting: Boolean = false,
        val isRawManifestVisible: Boolean = false,
        val previewType: PreviewType,
        val transactionFees: TransactionFees = TransactionFees(),
        val feePayerSearchResult: FeePayerSearchResult? = null,
        val defaultSignersCount: Int = 0,
        val sheetState: Sheet = Sheet.None,
        private val latestFeesMode: Sheet.CustomizeFees.FeesMode = Sheet.CustomizeFees.FeesMode.Default,
        val error: UiMessage? = null,
        val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom(),
        val interactionState: InteractionState? = null
    ) : UiState {

        val requestNonNull: MessageFromDataChannel.IncomingRequest.TransactionRequest
            get() = requireNotNull(request)

        fun noneRequiredState(): State = copy(
            sheetState = Sheet.CustomizeFees(
                feePayerMode = Sheet.CustomizeFees.FeePayerMode.NoFeePayerRequired,
                feesMode = latestFeesMode
            )
        )

        fun candidateSelectedState(feePayerCandidate: Network.Account): State = copy(
            sheetState = Sheet.CustomizeFees(
                feePayerMode = Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                    feePayerCandidate = feePayerCandidate
                ),
                feesMode = latestFeesMode
            )
        )

        fun noCandidateSelectedState(): State = copy(
            sheetState = Sheet.CustomizeFees(
                feePayerMode = Sheet.CustomizeFees.FeePayerMode.NoFeePayerSelected(
                    candidates = feePayerSearchResult?.candidates.orEmpty()
                ),
                feesMode = latestFeesMode
            )
        )

        fun feePayerSelectionState(): State = copy(
            transactionFees = transactionFees,
            sheetState = Sheet.CustomizeFees(
                feePayerMode = Sheet.CustomizeFees.FeePayerMode.SelectFeePayer(
                    candidates = feePayerSearchResult?.candidates.orEmpty()
                ),
                feesMode = latestFeesMode
            )
        )

        fun defaultModeState(): State = copy(
            transactionFees = transactionFees.copy(
                feePaddingAmount = null,
                tipPercentage = null
            ),
            latestFeesMode = Sheet.CustomizeFees.FeesMode.Default,
            sheetState = Sheet.CustomizeFees(
                feePayerMode = (sheetState as Sheet.CustomizeFees).feePayerMode,
                feesMode = Sheet.CustomizeFees.FeesMode.Default
            )
        )

        fun advancedModeState(): State = copy(
            latestFeesMode = Sheet.CustomizeFees.FeesMode.Advanced,
            sheetState = Sheet.CustomizeFees(
                feePayerMode = (sheetState as Sheet.CustomizeFees).feePayerMode,
                feesMode = Sheet.CustomizeFees.FeesMode.Advanced
            )
        )

        val isRawManifestToggleVisible: Boolean
            get() = previewType is PreviewType.Transfer

        val rawManifest: String = request?.transactionManifestData?.toTransactionManifest()?.getOrNull()?.toPrettyString().orEmpty()

        val isSheetVisible: Boolean
            get() = sheetState != Sheet.None

        val message: String?
            get() {
                val message = request?.transactionManifestData?.message
                return if (!message.isNullOrBlank()) {
                    message
                } else {
                    null
                }
            }

        val isSubmitEnabled: Boolean
            get() = previewType !is PreviewType.None && !isBalanceInsufficientToPayTheFee

        val noFeePayerSelected: Boolean
            get() = feePayerSearchResult?.feePayerAddress == null

        val isBalanceInsufficientToPayTheFee: Boolean
            get() {
                if (feePayerSearchResult == null) return true
                val candidateAddress = feePayerSearchResult.feePayerAddress ?: return true

                val xrdInCandidateAccount = feePayerSearchResult.candidates.find {
                    it.account.address == candidateAddress
                }?.xrdAmount ?: BigDecimal.ZERO

                // Calculate how many XRD have been used from accounts withdrawn from
                // In cases were it is not a transfer type, then it means the user
                // will not spend any other XRD rather than the ones spent for the fees
                val xrdUsed = when (previewType) {
                    is PreviewType.Transfer -> {
                        val candidateAddressWithdrawn = previewType.from.find { it.address == candidateAddress }
                        if (candidateAddressWithdrawn != null) {
                            val xrdResourceWithdrawn = candidateAddressWithdrawn.resources.map {
                                it.transferable
                            }.filterIsInstance<TransferableResource.Amount>().find { it.resource.isXrd }

                            xrdResourceWithdrawn?.amount ?: BigDecimal.ZERO
                        } else {
                            BigDecimal.ZERO
                        }
                    }
                    // On-purpose made this check exhaustive, future types may involve accounts spending XRD
                    is PreviewType.AccountsDepositSettings -> BigDecimal.ZERO
                    is PreviewType.NonConforming -> BigDecimal.ZERO
                    is PreviewType.None -> BigDecimal.ZERO
                    is PreviewType.UnacceptableManifest -> BigDecimal.ZERO
                }

                return xrdInCandidateAccount - xrdUsed < transactionFees.transactionFeeToLock
            }

        sealed interface Sheet {

            data object None : Sheet

//            data class ResourceDetails(
//                val resource: Resource, // it can be token or nft
//                val item: Resource.NonFungibleResource.Item? = null // if resource nft then pass item the user clicked
//            ) : Sheet()

            sealed interface ResourceSelected : Sheet {

                data class Fungible(val token: FungibleResource) : ResourceSelected

                data class NonFungible(
                    val collection: NonFungibleResource,
                    val item: NonFungibleResource.Item
                ) : ResourceSelected
            }

            data class CustomizeGuarantees(
                val accountsWithPredictedGuarantees: List<AccountWithPredictedGuarantee>
            ) : Sheet

            data class CustomizeFees(
                val feePayerMode: FeePayerMode,
                val feesMode: FeesMode
            ) : Sheet {

                sealed interface FeePayerMode {

                    data object NoFeePayerRequired : FeePayerMode

                    data class FeePayerSelected(
                        val feePayerCandidate: Network.Account
                    ) : FeePayerMode

                    data class NoFeePayerSelected(
                        val candidates: List<FeePayerSearchResult.FeePayerCandidate>
                    ) : FeePayerMode

                    data class SelectFeePayer(
                        val candidates: List<FeePayerSearchResult.FeePayerCandidate>
                    ) : FeePayerMode
                }

                enum class FeesMode {
                    Default, Advanced
                }
            }

            data class Dapp(
                val dApp: DAppWithMetadataAndAssociatedResources
            ) : Sheet
        }
    }

    sealed interface Event : OneOffEvent {
        data object Dismiss : Event
    }
}

sealed interface PreviewType {
    data object None : PreviewType

    data object UnacceptableManifest : PreviewType

    data object NonConforming : PreviewType

    data class AccountsDepositSettings(
        val accountsWithDepositSettingsChanges: List<AccountWithDepositSettingsChanges> = emptyList()
    ) : PreviewType

    data class Transfer(
        val from: List<AccountWithTransferableResources>,
        val to: List<AccountWithTransferableResources>,
        val badges: List<Badge> = emptyList(),
        val dApps: List<DAppWithMetadataAndAssociatedResources> = emptyList()
    ) : PreviewType
}

data class AccountWithDepositSettingsChanges(
    val account: Network.Account,
    val defaultDepositRule: AccountDefaultDepositRule? = null,
    val assetChanges: List<AssetPreferenceChange> = emptyList(),
    val depositorChanges: List<DepositorPreferenceChange> = emptyList()
) {
    data class AssetPreferenceChange(
        val change: ChangeType,
        val resource: Resource? = null
    ) {
        enum class ChangeType {
            Allow, Disallow, Clear
        }
    }

    data class DepositorPreferenceChange(
        val change: ChangeType,
        val resource: Resource? = null
    ) {
        enum class ChangeType {
            Add, Remove
        }
    }

    val onlyDepositRuleChanged: Boolean
        get() = assetChanges.isEmpty() && depositorChanges.isEmpty()
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

fun List<AccountWithTransferableResources>.guaranteesCount(): Int = map { accountWithTransferableResources ->
    accountWithTransferableResources.resources.filter { transferable ->
        transferable.guaranteeAssertion is GuaranteeAssertion.ForAmount
    }
}.flatten().count()
