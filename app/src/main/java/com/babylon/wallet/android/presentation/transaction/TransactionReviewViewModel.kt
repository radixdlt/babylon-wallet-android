package com.babylon.wallet.android.presentation.transaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.data.transaction.model.TransactionFeePayers
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.SignTransactionUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.transaction.analysis.TransactionAnalysisDelegate
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFeesDelegate
import com.babylon.wallet.android.presentation.transaction.guarantees.TransactionGuaranteesDelegate
import com.babylon.wallet.android.presentation.transaction.submit.TransactionSubmitDelegate
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.clamped
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.div
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.minus
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.rounded
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.toDecimal192OrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Divisibility
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.roundedWith
import rdx.works.core.mapWhen
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class TransactionReviewViewModel @Inject constructor(
    private val signTransactionUseCase: SignTransactionUseCase,
    private val analysis: TransactionAnalysisDelegate,
    private val guarantees: TransactionGuaranteesDelegate,
    private val fees: TransactionFeesDelegate,
    private val submit: TransactionSubmitDelegate,
    private val getDAppsUseCase: GetDAppsUseCase,
    incomingRequestRepository: IncomingRequestRepository,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<State>() {

    private val args = TransactionReviewArgs(savedStateHandle)

    override fun initialState(): State = State(
        isLoading = true,
        isNetworkFeeLoading = true,
        previewType = PreviewType.None
    )

    init {
        analysis(scope = viewModelScope, state = _state)
        guarantees(scope = viewModelScope, state = _state)
        fees(scope = viewModelScope, state = _state)
        submit(scope = viewModelScope, state = _state)

        val request = incomingRequestRepository.getTransactionWriteRequest(args.requestId)
        if (request == null) {
            viewModelScope.launch {
                _state.update { state ->
                    state.copy(isTransactionDismissed = true)
                }
            }
        } else {
            _state.update { it.copy(request = request) }
            viewModelScope.launch {
                signTransactionUseCase.signingState.collect { signingState ->
                    _state.update { state ->
                        state.copy(interactionState = signingState)
                    }
                }
            }
            viewModelScope.launch {
                analysis.analyse()
            }

            if (!request.isInternal) {
                viewModelScope.launch {
                    getDAppsUseCase(AccountAddress.init(request.requestMetadata.dAppDefinitionAddress), false).onSuccess { dApp ->
                        _state.update { it.copy(proposingDApp = dApp) }
                    }
                }
            }
        }
    }

    fun onBackClick() {
        if (state.value.sheetState != State.Sheet.None) {
            _state.update { it.copy(sheetState = State.Sheet.None) }
        } else {
            viewModelScope.launch {
                submit.onDismiss(
                    signTransactionUseCase = signTransactionUseCase,
                    exception = RadixWalletException.DappRequestException.RejectedByUser
                )
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
        submit.onSubmit(
            signTransactionUseCase = signTransactionUseCase,
            deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
        )
    }

    fun promptForGuaranteesClick() = guarantees.onEdit()

    fun onGuaranteeValueChange(account: AccountWithPredictedGuarantee, value: String) = guarantees.onValueChange(
        account = account,
        value = value
    )

    fun onGuaranteeValueIncreased(account: AccountWithPredictedGuarantee) = guarantees.onValueIncreased(account)

    fun onGuaranteeValueDecreased(account: AccountWithPredictedGuarantee) = guarantees.onValueDecreased(account)

    fun onGuaranteesApplyClick() = guarantees.onApply()

    fun onCloseBottomSheetClick() {
        _state.update { it.copy(sheetState = State.Sheet.None) }
    }

    fun onCustomizeClick() {
        viewModelScope.launch {
            fees.onCustomizeClick()
        }
    }

    fun onCancelSigningClick() {
        signTransactionUseCase.cancelSigning()
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

    fun onPayerSelected(selectedFeePayer: Account) {
        val feePayerSearchResult = state.value.feePayers
        val transactionFees = state.value.transactionFees
        val signersCount = state.value.defaultSignersCount

        val updatedFeePayerResult = feePayerSearchResult?.copy(
            selectedAccountAddress = selectedFeePayer.address,
            candidates = feePayerSearchResult.candidates
        )

        val customizeFeesSheet = state.value.sheetState as? State.Sheet.CustomizeFees ?: return
        val selectedFeePayerInvolvedInTransaction = state.value.request?.transactionManifestData?.feePayerCandidates()
            .orEmpty().any { accountAddress ->
                accountAddress == selectedFeePayer.address
            }

        val updatedSignersCount = if (selectedFeePayerInvolvedInTransaction) signersCount else signersCount + 1

        _state.update {
            it.copy(
                transactionFees = transactionFees.copy(
                    signersCount = updatedSignersCount
                ),
                feePayers = updatedFeePayerResult,
                sheetState = customizeFeesSheet.copy(
                    feePayerMode = State.Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                        feePayerCandidate = selectedFeePayer
                    )
                )
            )
        }
    }

    fun onUnknownAddressesClick(unknownAddresses: ImmutableList<Address>) {
        _state.update {
            it.copy(sheetState = State.Sheet.UnknownAddresses(unknownAddresses))
        }
    }

    fun dismissTerminalErrorDialog() {
        _state.update { it.copy(error = null) }
        onBackClick()
    }

    fun onAcknowledgeRawTransactionWarning() {
        _state.update { it.copy(showRawTransactionWarning = false) }
    }

    data class State(
        val request: MessageFromDataChannel.IncomingRequest.TransactionRequest? = null,
        val proposingDApp: DApp? = null,
        val endEpoch: ULong? = null,
        val isLoading: Boolean,
        val isNetworkFeeLoading: Boolean,
        val isSubmitting: Boolean = false,
        val isRawManifestVisible: Boolean = false,
        val showRawTransactionWarning: Boolean = false,
        val previewType: PreviewType,
        val transactionFees: TransactionFees = TransactionFees(),
        val feePayers: TransactionFeePayers? = null,
        val defaultSignersCount: Int = 0,
        val sheetState: Sheet = Sheet.None,
        private val latestFeesMode: Sheet.CustomizeFees.FeesMode = Sheet.CustomizeFees.FeesMode.Default,
        val error: TransactionErrorMessage? = null,
        val ephemeralNotaryPrivateKey: Curve25519SecretKey = Curve25519SecretKey.secureRandom(),
        val interactionState: InteractionState? = null,
        val isTransactionDismissed: Boolean = false
    ) : UiState {

        val requestNonNull: MessageFromDataChannel.IncomingRequest.TransactionRequest
            get() = requireNotNull(request)

        fun noneRequiredState(): State = copy(
            sheetState = Sheet.CustomizeFees(
                feePayerMode = Sheet.CustomizeFees.FeePayerMode.NoFeePayerRequired,
                feesMode = latestFeesMode
            )
        )

        fun candidateSelectedState(feePayerCandidate: Account): State = copy(
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
                    candidates = feePayers?.candidates.orEmpty()
                ),
                feesMode = latestFeesMode
            )
        )

        fun feePayerSelectionState(): State = copy(
            transactionFees = transactionFees,
            sheetState = Sheet.CustomizeFees(
                feePayerMode = Sheet.CustomizeFees.FeePayerMode.SelectFeePayer(
                    candidates = feePayers?.candidates.orEmpty()
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

        val rawManifest: String = request
            ?.transactionManifestData
            ?.instructions.orEmpty()

        val isSheetVisible: Boolean
            get() = sheetState != Sheet.None

        val message: String?
            get() = request?.transactionManifestData?.message?.messageOrNull

        val isSubmitEnabled: Boolean
            get() = previewType !is PreviewType.None && !isBalanceInsufficientToPayTheFee

        val noFeePayerSelected: Boolean
            get() = feePayers?.selectedAccountAddress == null

        val isBalanceInsufficientToPayTheFee: Boolean
            get() {
                if (feePayers == null) return true
                val candidateAddress = feePayers.selectedAccountAddress ?: return true

                val xrdInCandidateAccount = feePayers.candidates.find {
                    it.account.address == candidateAddress
                }?.xrdAmount.orZero()

                // Calculate how many XRD have been used from accounts withdrawn from
                // In cases were it is not a transfer type, then it means the user
                // will not spend any other XRD rather than the ones spent for the fees
                val xrdUsed = when (previewType) {
                    is PreviewType.Transfer.GeneralTransfer -> {
                        val candidateAddressWithdrawn = previewType.from.find { it.address == candidateAddress }
                        if (candidateAddressWithdrawn != null) {
                            val xrdResourceWithdrawn = candidateAddressWithdrawn.resources.map {
                                it.transferable
                            }.filterIsInstance<TransferableAsset.Fungible.Token>().find { it.resource.isXrd }

                            xrdResourceWithdrawn?.amount.orZero()
                        } else {
                            0.toDecimal192()
                        }
                    }
                    // On-purpose made this check exhaustive, future types may involve accounts spending XRD
                    is PreviewType.AccountsDepositSettings -> 0.toDecimal192()
                    is PreviewType.NonConforming -> 0.toDecimal192()
                    is PreviewType.None -> 0.toDecimal192()
                    is PreviewType.UnacceptableManifest -> 0.toDecimal192()
                    is PreviewType.Transfer.Pool -> 0.toDecimal192()
                    is PreviewType.Transfer.Staking -> 0.toDecimal192()
                }

                return xrdInCandidateAccount - xrdUsed < transactionFees.transactionFeeToLock
            }

        val showDottedLine: Boolean
            get() = when (previewType) {
                is PreviewType.Transfer -> {
                    previewType.from.isNotEmpty() && previewType.to.isNotEmpty()
                }
                else -> false
            }

        sealed interface Sheet {

            data object None : Sheet

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
                        val feePayerCandidate: Account
                    ) : FeePayerMode

                    data class NoFeePayerSelected(
                        val candidates: List<TransactionFeePayers.FeePayerCandidate>
                    ) : FeePayerMode

                    data class SelectFeePayer(
                        val candidates: List<TransactionFeePayers.FeePayerCandidate>
                    ) : FeePayerMode
                }

                enum class FeesMode {
                    Default, Advanced
                }
            }

            data class UnknownAddresses(
                val unknownAddresses: ImmutableList<Address>
            ) : Sheet
        }
    }
}

data class TransactionErrorMessage(
    private val error: Throwable?
) {

    private val isNoMnemonicErrorVisible = error?.cause is ProfileException.NoMnemonic

    /**
     * True when this error will end up abandoning the transaction. Displayed as a dialog.
     */
    val isTerminalError: Boolean
        get() = isNoMnemonicErrorVisible ||
            error is RadixWalletException.PrepareTransactionException.ReceivingAccountDoesNotAllowDeposits ||
            error is RadixWalletException.PrepareTransactionException.FailedToFindSigningEntities ||
            error is RadixWalletException.LedgerCommunicationException.FailedToSignTransaction

    val uiMessage: UiMessage = UiMessage.ErrorMessage(error)

    @Composable
    fun getTitle(): String {
        return if (isNoMnemonicErrorVisible) {
            stringResource(id = R.string.transactionReview_noMnemonicError_title)
        } else if (error is RadixWalletException.LedgerCommunicationException.FailedToSignTransaction) {
            stringResource(id = R.string.ledgerHardwareDevices_couldNotSign_title)
        } else {
            stringResource(id = R.string.common_errorAlertTitle)
        }
    }
}

sealed interface PreviewType {
    data object None : PreviewType

    data object UnacceptableManifest : PreviewType

    data object NonConforming : PreviewType

    data class AccountsDepositSettings(
        val accountsWithDepositSettingsChanges: List<AccountWithDepositSettingsChanges> = emptyList()
    ) : PreviewType {
        val hasSettingSection: Boolean
            get() = accountsWithDepositSettingsChanges.any { it.defaultDepositRule != null }

        val hasExceptionsSection: Boolean
            get() = accountsWithDepositSettingsChanges.any { it.assetChanges.isNotEmpty() || it.depositorChanges.isNotEmpty() }
    }

    sealed interface Transfer : PreviewType {
        val from: List<AccountWithTransferableResources>
        val to: List<AccountWithTransferableResources>

        val newlyCreatedResources: List<Resource>
            get() = (from + to).map { allTransfers ->
                allTransfers.resources.filter { it.transferable.isNewlyCreated }.map { it.transferable.resource }
            }.flatten()

        data class Staking(
            override val from: List<AccountWithTransferableResources>,
            override val to: List<AccountWithTransferableResources>,
            val validators: List<Validator>,
            val actionType: ActionType
        ) : Transfer {
            enum class ActionType {
                Stake, Unstake, ClaimStake
            }
        }

        data class Pool(
            override val from: List<AccountWithTransferableResources>,
            override val to: List<AccountWithTransferableResources>,
            val actionType: ActionType
        ) : Transfer {
            enum class ActionType {
                Contribution, Redemption
            }

            val poolsInvolved: Set<rdx.works.core.domain.resources.Pool>
                get() = (from + to).toSet().map { accountWithAssets ->
                    accountWithAssets.resources.mapNotNull {
                        (it.transferable as? TransferableAsset.Fungible.PoolUnitAsset)?.unit?.pool
                    }
                }.flatten().toSet()
        }

        data class GeneralTransfer(
            override val from: List<AccountWithTransferableResources>,
            override val to: List<AccountWithTransferableResources>,
            val badges: List<Badge> = emptyList(),
            val dApps: List<Pair<ComponentAddress, DApp?>> = emptyList()
        ) : Transfer
    }
}

data class AccountWithDepositSettingsChanges(
    val account: Account,
    val defaultDepositRule: DepositRule? = null,
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
    val address: AccountAddress
    val transferable: TransferableAsset.Fungible
    val instructionIndex: Long
    val guaranteeAmountString: String

    val guaranteeOffsetDecimal: Decimal192
        get() = guaranteeAmountString.toDecimal192OrNull().orZero() / 100.toDecimal192()

    val guaranteedAmount: Decimal192
        get() = (transferable.amount * guaranteeOffsetDecimal).roundedWith(divisibility)

    private val divisibility: Divisibility?
        get() = when (val asset = transferable) {
            is TransferableAsset.Fungible.Token -> {
                asset.resource.divisibility
            }
            is TransferableAsset.Fungible.LSUAsset -> {
                asset.resource.divisibility
            }
            is TransferableAsset.Fungible.PoolUnitAsset -> {
                asset.resource.divisibility
            }
        }

    fun increase(): AccountWithPredictedGuarantee {
        val newOffset = ((guaranteeOffsetDecimal + changeOffset) * 100.toDecimal192()).rounded(decimalPlaces = 1u)
        return when (this) {
            is Other -> copy(guaranteeAmountString = newOffset.formatted())
            is Owned -> copy(guaranteeAmountString = newOffset.formatted())
        }
    }

    fun decrease(): AccountWithPredictedGuarantee {
        val newOffset = ((guaranteeOffsetDecimal - changeOffset).clamped * 100.toDecimal192()).rounded(decimalPlaces = 1u)
        return when (this) {
            is Other -> copy(guaranteeAmountString = newOffset.formatted())
            is Owned -> copy(guaranteeAmountString = newOffset.formatted())
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

    fun isTheSameGuaranteeItem(with: AccountWithPredictedGuarantee): Boolean = address == with.address &&
        transferable.resourceAddress == with.transferable.resourceAddress

    data class Owned(
        val account: Account,
        override val transferable: TransferableAsset.Fungible,
        override val instructionIndex: Long,
        override val guaranteeAmountString: String
    ) : AccountWithPredictedGuarantee {
        override val address: AccountAddress
            get() = account.address
    }

    data class Other(
        override val address: AccountAddress,
        override val transferable: TransferableAsset.Fungible,
        override val instructionIndex: Long,
        override val guaranteeAmountString: String
    ) : AccountWithPredictedGuarantee

    companion object {
        private val changeOffset = 0.001.toDecimal192()
    }
}

sealed interface AccountWithTransferableResources {

    val address: AccountAddress
    val resources: List<Transferable>

    data class Owned(
        val account: Account,
        override val resources: List<Transferable>
    ) : AccountWithTransferableResources {
        override val address: AccountAddress
            get() = account.address
    }

    data class Other(
        override val address: AccountAddress,
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
                    it.address == address && it.transferable.resourceAddress == depositing.transferable.resourceAddress
                }
            },
            mutation = { depositing ->
                val accountWithGuarantee = resourcesWithGuaranteesForAccount.find {
                    it.transferable.resourceAddress == depositing.transferable.resourceAddress
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

    companion object {
        class Sorter(
            private val ownedAccountsOrder: List<Account>
        ) : Comparator<AccountWithTransferableResources> {
            override fun compare(thisAccount: AccountWithTransferableResources?, otherAccount: AccountWithTransferableResources?): Int {
                val indexOfThisAccount = ownedAccountsOrder.indexOfFirst { it.address == thisAccount?.address }
                val indexOfOtherAccount = ownedAccountsOrder.indexOfFirst { it.address == otherAccount?.address }

                return if (indexOfThisAccount == -1 && indexOfOtherAccount >= 0) {
                    1 // The other account is owned, so it takes higher priority
                } else if (indexOfOtherAccount == -1 && indexOfThisAccount >= 0) {
                    -1 // This account is owned, so it takes higher priority
                } else if (indexOfThisAccount == -1 && indexOfOtherAccount == -1) {
                    0 // Both accounts are not owned, both considered equal, so they will be sorted according to the receiving order
                } else {
                    indexOfThisAccount - indexOfOtherAccount
                }
            }
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
