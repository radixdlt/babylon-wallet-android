package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.model.transaction.TransactionToReviewData
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.TransactionFeePayers
import com.babylon.wallet.android.domain.usecases.signing.NotaryAndSigners
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
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFeesDelegateImpl
import com.babylon.wallet.android.presentation.transaction.guarantees.TransactionGuaranteesDelegate
import com.babylon.wallet.android.presentation.transaction.guarantees.TransactionGuaranteesDelegateImpl
import com.babylon.wallet.android.presentation.transaction.model.AccountWithDepositSettingsChanges
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.GuaranteeItem
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.transaction.submit.TransactionSubmitDelegate
import com.babylon.wallet.android.presentation.transaction.submit.TransactionSubmitDelegateImpl
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ManifestEncounteredComponentAddress
import com.radixdlt.sargon.ResourceIdentifier
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.hiddenResources
import com.radixdlt.sargon.extensions.init
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.sargon.getResourcePreferences
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class TransactionReviewViewModel @Inject constructor(
    private val appEventBus: AppEventBus,
    private val analysis: TransactionAnalysisDelegate,
    private val guarantees: TransactionGuaranteesDelegateImpl,
    private val fees: TransactionFeesDelegateImpl,
    private val submit: TransactionSubmitDelegateImpl,
    private val getDAppsUseCase: GetDAppsUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val getProfileUseCase: GetProfileUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<State>(),
    OneOffEventHandler<TransactionReviewViewModel.Event> by OneOffEventHandlerImpl(),
    TransactionGuaranteesDelegate by guarantees,
    TransactionFeesDelegate by fees,
    TransactionSubmitDelegate by submit {

    private val args = TransactionReviewArgs(savedStateHandle)
    private val data = MutableStateFlow(Data())

    override fun initialState(): State = State(
        isLoading = true,
        previewType = PreviewType.None
    )

    init {
        initHiddenResources()

        analysis(scope = viewModelScope, data = data, state = _state)
        guarantees(scope = viewModelScope, state = _state)
        fees(scope = viewModelScope, data = data, state = _state)
        submit(scope = viewModelScope, data = data, state = _state)
        submit.oneOffEventHandler = this

        observeDeferredRequests()
        processIncomingRequest()
    }

    private fun initHiddenResources() {
        viewModelScope.launch {
            withContext(defaultDispatcher) {
                _state.update {
                    it.copy(
                        hiddenResourceIds = getProfileUseCase().getResourcePreferences().hiddenResources
                            .toPersistentList()
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
        val request = incomingRequestRepository.getRequest(args.interactionId) as? TransactionRequest
        if (request == null) {
            viewModelScope.launch {
                sendEvent(Event.Dismiss)
            }
        } else {
            data.update { it.copy(txRequest = request) }
            _state.update {
                it.copy(
                    rawManifest = request.unvalidatedManifestData.instructions,
                    message = request.unvalidatedManifestData.plainMessage
                )
            }

            viewModelScope.launch {
                withContext(defaultDispatcher) {
                    analysis.analyse()
                    // TODO call this only if it's a regular transaction
                    fees.resolveFees()
                }
            }

            if (request.isInternal) {
                _state.update { it.copy(proposingDApp = State.ProposingDApp.None) }
            } else {
                viewModelScope.launch {
                    getDAppsUseCase(AccountAddress.init(request.requestMetadata.dAppDefinitionAddress), false)
                        .onSuccess { dApp ->
                            _state.update {
                                it.copy(proposingDApp = State.ProposingDApp.Some(dApp))
                            }
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

    fun onCloseBottomSheetClick() {
        _state.update { it.copy(sheetState = Sheet.None) }
    }

    fun onUnknownAddressesClick(unknownAddresses: ImmutableList<Address>) {
        _state.update {
            it.copy(sheetState = Sheet.UnknownAddresses(unknownAddresses))
        }
    }

    fun dismissTerminalErrorDialog() {
        (state.value.error?.error as? RadixWalletException.DappRequestException)?.let { exception ->
            viewModelScope.launch { submit.onDismiss(exception) }
        }
        _state.update { it.copy(error = null) }
    }

    fun onAcknowledgeRawTransactionWarning() {
        _state.update { it.copy(showRawTransactionWarning = false) }
    }

    data class Data(
        private val txRequest: TransactionRequest? = null,
        private val txToReviewData: TransactionToReviewData? = null,
        private val txNotaryAndSigners: NotaryAndSigners? = null,
        val ephemeralNotaryPrivateKey: Curve25519SecretKey = Curve25519SecretKey.secureRandom(),
        val endEpoch: ULong? = null,
        val latestFeesMode: Sheet.CustomizeFees.FeesMode = Sheet.CustomizeFees.FeesMode.Default,
        val feePayers: TransactionFeePayers? = null,
        val transactionFees: TransactionFees? = null
    ) {

        val request: TransactionRequest
            get() = requireNotNull(txRequest)
        val transactionToReviewData: TransactionToReviewData
            get() = requireNotNull(txToReviewData)
        val notaryAndSigners: NotaryAndSigners
            get() = requireNotNull(txNotaryAndSigners)

        val feePayerCandidates: List<AccountAddress> by lazy {
            val manifestSummary = transactionToReviewData.manifestSummary
            manifestSummary.addressesOfAccountsWithdrawnFrom +
                manifestSummary.addressesOfAccountsDepositedInto +
                manifestSummary.addressesOfAccountsRequiringAuth
        }
    }

    data class State(
        val isLoading: Boolean,
        val transactionType: TransactionType? = null,
        val proposingDApp: ProposingDApp? = null,
        val isRawManifestVisible: Boolean = false,
        val rawManifest: String = "",
        val showRawTransactionWarning: Boolean = false,
        val message: String? = null,
        val previewType: PreviewType,
        val sheetState: Sheet = Sheet.None,
        val fees: Fees? = null,
        val preAuthorization: PreAuthorization? = null,
        val error: TransactionErrorMessage? = null,
        val hiddenResourceIds: PersistentList<ResourceIdentifier> = persistentListOf(),
        val isSubmitEnabled: Boolean = false,
        val isSubmitting: Boolean = false
    ) : UiState {

        val isRawManifestToggleVisible: Boolean
            get() = previewType is PreviewType.Transfer

        val isSheetVisible: Boolean
            get() = sheetState != Sheet.None

        val showDottedLine: Boolean
            get() = when (previewType) {
                is PreviewType.Transfer -> {
                    previewType.from.isNotEmpty() && previewType.to.isNotEmpty()
                }

                else -> false
            }

        val showReceiptEdges: Boolean
            get() = transactionType == TransactionType.Regular

        enum class TransactionType {
            Regular,
            PreAuthorized
        }

        data class Fees(
            val isNetworkFeeLoading: Boolean = true,
            val properties: Properties = Properties(),
            val transactionFees: TransactionFees = TransactionFees(),
            val selectedFeePayerInput: SelectFeePayerInput? = null
        ) {

            data class Properties(
                val isSelectedFeePayerInvolvedInTransaction: Boolean = true,
                val noFeePayerSelected: Boolean = false,
                val isBalanceInsufficientToPayTheFee: Boolean = false
            )
        }

        data class PreAuthorization(
            val validFor: String
        )

        sealed interface ProposingDApp {

            val name: String?
                get() = when (this) {
                    is Some -> dApp?.name
                    None -> null
                }

            data object None : ProposingDApp

            data class Some(val dApp: DApp?) : ProposingDApp
        }

        interface Sheet {

            data object None : Sheet

            data class CustomizeGuarantees(val guarantees: List<GuaranteeItem>) : Sheet {

                val isSubmitEnabled: Boolean = guarantees.all { it.isInputValid }

            }

            data class CustomizeFees(
                val feePayerMode: FeePayerMode,
                val feesMode: FeesMode,
                val transactionFees: TransactionFees,
                val properties: Fees.Properties
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

    sealed interface Event : OneOffEvent {
        data object Dismiss : Event
    }
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
        val from: List<AccountWithTransferables>
        val to: List<AccountWithTransferables>
        val newlyCreatedNFTItems: List<Resource.NonFungibleResource.Item>

        val newlyCreatedResources: List<Resource>
            get() = (from + to).map { allTransfers ->
                allTransfers.transferables.filter { it.isNewlyCreated }.map { it.asset.resource }
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
            override val from: List<AccountWithTransferables>,
            override val to: List<AccountWithTransferables>,
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
            override val from: List<AccountWithTransferables>,
            override val to: List<AccountWithTransferables>,
            override val badges: List<Badge>,
            val actionType: ActionType,
            override val newlyCreatedNFTItems: List<Resource.NonFungibleResource.Item>
        ) : Transfer {
            enum class ActionType {
                Contribution, Redemption
            }

            val poolsInvolved: Set<rdx.works.core.domain.resources.Pool>
                get() = (from + to).toSet().map { accountWithAssets ->
                    accountWithAssets.transferables.mapNotNull {
                        (it as? Transferable.FungibleType.PoolUnit)?.asset?.pool
                    }
                }.flatten().toSet()
        }

        data class GeneralTransfer(
            override val from: List<AccountWithTransferables>,
            override val to: List<AccountWithTransferables>,
            override val badges: List<Badge> = emptyList(),
            val dApps: List<Pair<ManifestEncounteredComponentAddress, DApp?>> = emptyList(),
            override val newlyCreatedNFTItems: List<Resource.NonFungibleResource.Item>
        ) : Transfer
    }
}
