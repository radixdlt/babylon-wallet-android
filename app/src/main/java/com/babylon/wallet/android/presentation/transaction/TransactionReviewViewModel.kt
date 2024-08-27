package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.transaction.model.TransactionFeePayers
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State.Sheet
import com.babylon.wallet.android.presentation.transaction.analysis.TransactionAnalysisDelegate
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFeesDelegate
import com.babylon.wallet.android.presentation.transaction.guarantees.TransactionGuaranteesDelegate
import com.babylon.wallet.android.presentation.transaction.model.AccountWithDepositSettingsChanges
import com.babylon.wallet.android.presentation.transaction.model.AccountWithPredictedGuarantee
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.babylon.wallet.android.presentation.transaction.submit.TransactionSubmitDelegate
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.ResourceIdentifier
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.minus
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.toDecimal192
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.sargon.hidden
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class TransactionReviewViewModel @Inject constructor(
    private val appEventBus: AppEventBus,
    private val analysis: TransactionAnalysisDelegate,
    private val guarantees: TransactionGuaranteesDelegate,
    private val fees: TransactionFeesDelegate,
    private val submit: TransactionSubmitDelegate,
    private val getDAppsUseCase: GetDAppsUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val getProfileUseCase: GetProfileUseCase,
    @DefaultDispatcher private val coroutineDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<State>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = TransactionReviewArgs(savedStateHandle)

    override fun initialState(): State = State(
        isLoading = true,
        isNetworkFeeLoading = true,
        previewType = PreviewType.None
    )

    init {
        initState()

        analysis(scope = viewModelScope, state = _state)
        guarantees(scope = viewModelScope, state = _state)
        fees(scope = viewModelScope, state = _state)
        submit(scope = viewModelScope, state = _state)
        submit.oneOffEventHandler = this

        observeDeferredRequests()
        processIncomingRequest()
    }

    private fun initState() {
        viewModelScope.launch {
            withContext(coroutineDispatcher) {
                _state.update {
                    it.copy(
                        hiddenResourceIds = getProfileUseCase().appPreferences.resources.hidden().toPersistentList()
                    )
                }
            }
        }
    }

    private fun observeDeferredRequests() {
        viewModelScope.launch {
            appEventBus.events.filterIsInstance<AppEvent.DeferRequestHandling>().collect {
                if (it.interactionId == args.interactionId) {
                    sendEvent(Event.Dismiss)
                    incomingRequestRepository.requestDeferred(args.interactionId)
                }
            }
        }
    }

    private fun processIncomingRequest() {
        val request = incomingRequestRepository.getRequest(args.interactionId) as? IncomingMessage.IncomingRequest.TransactionRequest
        if (request == null) {
            viewModelScope.launch {
                sendEvent(Event.Dismiss)
            }
        } else {
            _state.update { it.copy(request = request) }
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
        if (state.value.sheetState != Sheet.None) {
            _state.update { it.copy(sheetState = Sheet.None) }
        } else {
            viewModelScope.launch {
                submit.onDismiss(exception = RadixWalletException.DappRequestException.RejectedByUser)
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    fun onRawManifestToggle() {
        _state.update { it.copy(isRawManifestVisible = !it.isRawManifestVisible) }
    }

    fun onApproveTransaction() = submit.onSubmit()

    fun promptForGuaranteesClick() = guarantees.onEdit()

    fun onGuaranteeValueChange(account: AccountWithPredictedGuarantee, value: String) = guarantees.onValueChange(
        account = account,
        value = value
    )

    fun onGuaranteeValueIncreased(account: AccountWithPredictedGuarantee) = guarantees.onValueIncreased(account)

    fun onGuaranteeValueDecreased(account: AccountWithPredictedGuarantee) = guarantees.onValueDecreased(account)

    fun onGuaranteesApplyClick() = guarantees.onApply()

    fun onCloseBottomSheetClick() {
        _state.update { it.copy(sheetState = Sheet.None) }
    }

    fun onCustomizeClick() = viewModelScope.launch {
        fees.onCustomizeClick()
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

    fun onPayerChanged(selectedFeePayer: TransactionFeePayers.FeePayerCandidate) {
        val selectFeePayerInput = state.value.selectedFeePayerInput ?: return

        _state.update {
            it.copy(
                selectedFeePayerInput = selectFeePayerInput.copy(
                    preselectedCandidate = selectedFeePayer
                )
            )
        }
    }

    fun onPayerSelected() {
        val selectFeePayerInput = state.value.selectedFeePayerInput ?: return
        val selectedFeePayerAccount = selectFeePayerInput.preselectedCandidate?.account ?: return

        val feePayerSearchResult = state.value.feePayers
        val transactionFees = state.value.transactionFees
        val signersCount = state.value.defaultSignersCount

        val updatedFeePayerResult = feePayerSearchResult?.copy(
            selectedAccountAddress = selectedFeePayerAccount.address,
            candidates = feePayerSearchResult.candidates
        )

        val customizeFeesSheet = state.value.sheetState as? Sheet.CustomizeFees ?: return
        val selectedFeePayerInvolvedInTransaction = state.value.request?.transactionManifestData?.feePayerCandidates()
            .orEmpty()
            .any { accountAddress ->
                accountAddress == selectedFeePayerAccount.address
            }

        val updatedSignersCount = if (selectedFeePayerInvolvedInTransaction) signersCount else signersCount + 1

        _state.update {
            it.copy(
                transactionFees = transactionFees.copy(
                    signersCount = updatedSignersCount
                ),
                feePayers = updatedFeePayerResult,
                sheetState = customizeFeesSheet.copy(
                    feePayerMode = Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                        feePayerCandidate = selectedFeePayerAccount
                    )
                )
            )
        }
    }

    fun onFeePayerSelectionDismissRequest() {
        _state.update { it.copy(selectedFeePayerInput = null) }
    }

    fun onUnknownAddressesClick(unknownAddresses: ImmutableList<Address>) {
        _state.update {
            it.copy(sheetState = Sheet.UnknownAddresses(unknownAddresses))
        }
    }

    fun dismissTerminalErrorDialog() {
        _state.update { it.copy(error = null) }
    }

    fun onAcknowledgeRawTransactionWarning() {
        _state.update { it.copy(showRawTransactionWarning = false) }
    }

    data class State(
        val request: IncomingMessage.IncomingRequest.TransactionRequest? = null,
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
        val selectedFeePayerInput: SelectFeePayerInput? = null,
        val hiddenResourceIds: PersistentList<ResourceIdentifier> = persistentListOf()
    ) : UiState {

        val requestNonNull: IncomingMessage.IncomingRequest.TransactionRequest
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
            selectedFeePayerInput = SelectFeePayerInput(
                preselectedCandidate = feePayers?.candidates?.firstOrNull { it.account.address == feePayers.selectedAccountAddress },
                candidates = feePayers?.candidates.orEmpty().toPersistentList(),
                fee = transactionFees.transactionFeeToLock.formatted()
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

        val rawManifest: String = request?.transactionManifestData?.instructions.orEmpty()

        val isSheetVisible: Boolean
            get() = sheetState != Sheet.None

        val message: String?
            get() = request?.transactionManifestData?.message?.messageOrNull

        val isSubmitEnabled: Boolean
            get() = previewType !is PreviewType.None && !isBalanceInsufficientToPayTheFee

        val noFeePayerSelected: Boolean
            get() = feePayers?.selectedAccountAddress == null

        val isSelectedFeePayerInvolvedInTransaction: Boolean
            get() = request?.transactionManifestData?.feePayerCandidates()?.contains(feePayers?.selectedAccountAddress) ?: false

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
                }

                enum class FeesMode {
                    Default, Advanced
                }
            }

            data class UnknownAddresses(
                val unknownAddresses: ImmutableList<Address>
            ) : Sheet
        }

        data class SelectFeePayerInput(
            val preselectedCandidate: TransactionFeePayers.FeePayerCandidate?,
            val candidates: PersistentList<TransactionFeePayers.FeePayerCandidate>,
            val fee: String
        )
    }
}

sealed interface Event : OneOffEvent {
    data object Dismiss : Event
}

sealed interface PreviewType {

    val badges: List<Badge>

    data object None : PreviewType {
        override val badges: List<Badge> = emptyList()
    }

    data object UnacceptableManifest : PreviewType {
        override val badges: List<Badge> = emptyList()
    }

    data object NonConforming : PreviewType {
        override val badges: List<Badge> = emptyList()
    }

    data class AccountsDepositSettings(
        val accountsWithDepositSettingsChanges: List<AccountWithDepositSettingsChanges> = emptyList(),
        override val badges: List<Badge>
    ) : PreviewType {
        val hasSettingSection: Boolean
            get() = accountsWithDepositSettingsChanges.any { it.defaultDepositRule != null }

        val hasExceptionsSection: Boolean
            get() = accountsWithDepositSettingsChanges.any { it.assetChanges.isNotEmpty() || it.depositorChanges.isNotEmpty() }
    }

    sealed interface Transfer : PreviewType {
        val from: List<AccountWithTransferableResources>
        val to: List<AccountWithTransferableResources>
        val newlyCreatedNFTItems: List<Resource.NonFungibleResource.Item>

        val newlyCreatedResources: List<Resource>
            get() = (from + to).map { allTransfers ->
                allTransfers.resources.filter { it.transferable.isNewlyCreated }.map { it.transferable.resource }
            }.flatten()

        val newlyCreatedNFTItemsForExistingResources: List<Resource.NonFungibleResource.Item>
            get() {
                val newlyCreatedNFTResources = newlyCreatedResources.filterIsInstance<Resource.NonFungibleResource>()
                val addresses = newlyCreatedNFTResources.map { it.address }
                return newlyCreatedNFTItems.filterNot { nftItem ->
                    val newResource = newlyCreatedNFTResources.find { resource ->
                        resource.address == nftItem.collectionAddress
                    }
                    nftItem.collectionAddress in addresses && nftItem.localId in newResource?.items?.map { it.localId }.orEmpty()
                }
            }

        data class Staking(
            override val from: List<AccountWithTransferableResources>,
            override val to: List<AccountWithTransferableResources>,
            override val badges: List<Badge>,
            val validators: List<Validator>,
            val actionType: ActionType,
            override val newlyCreatedNFTItems: List<Resource.NonFungibleResource.Item>
        ) : Transfer {
            enum class ActionType {
                Stake, Unstake, ClaimStake
            }
        }

        data class Pool(
            override val from: List<AccountWithTransferableResources>,
            override val to: List<AccountWithTransferableResources>,
            override val badges: List<Badge>,
            val actionType: ActionType,
            override val newlyCreatedNFTItems: List<Resource.NonFungibleResource.Item>
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
            override val badges: List<Badge> = emptyList(),
            val dApps: List<Pair<ComponentAddress, DApp?>> = emptyList(),
            override val newlyCreatedNFTItems: List<Resource.NonFungibleResource.Item>
        ) : Transfer
    }
}
