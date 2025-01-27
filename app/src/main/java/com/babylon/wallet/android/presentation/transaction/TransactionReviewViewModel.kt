package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.SubintentExpiration
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.RadixWalletException.DappRequestException
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.TransactionFeePayers
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State.Sheet
import com.babylon.wallet.android.presentation.transaction.analysis.TransactionAnalysisDelegate
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
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
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpirationStatus
import com.radixdlt.sargon.ManifestEncounteredComponentAddress
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceIdentifier
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.hiddenResources
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.sargon.getResourcePreferences
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val EXPIRATION_COUNTDOWN_PERIOD_MS = 1000L

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

    override fun initialState(): State = State(
        isLoading = true,
        previewType = PreviewType.None
    )

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

    private fun processIncomingRequest() = viewModelScope.launch {
        val request = incomingRequestRepository.getRequest(args.interactionId) as? TransactionRequest
        if (request == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    previewType = PreviewType.None,
                    error = TransactionErrorMessage(RadixWalletException.PrepareTransactionException.RequestNotFound)
                )
            }
        } else {
            data.update { it.copy(txRequest = request) }

            if (request.kind is TransactionRequest.Kind.PreAuthorized) {
                when (request.kind.expiration.toDAppInteraction().status) {
                    DappToWalletInteractionSubintentExpirationStatus.EXPIRATION_TOO_CLOSE -> {
                        _state.update {
                            it.copy(error = TransactionErrorMessage(DappRequestException.InvalidPreAuthorizationExpirationTooClose))
                        }
                        return@launch
                    }

                    DappToWalletInteractionSubintentExpirationStatus.EXPIRED -> {
                        _state.update {
                            it.copy(error = TransactionErrorMessage(DappRequestException.InvalidPreAuthorizationExpired))
                        }
                        return@launch
                    }

                    DappToWalletInteractionSubintentExpirationStatus.VALID -> {
                        // Nothing to do. Transaction is valid to be analysed.
                    }
                }
            }
            _state.update {
                it.copy(
                    isPreAuthorization = request.kind.isPreAuthorized,
                    rawManifest = request.unvalidatedManifestData.instructions,
                    message = request.unvalidatedManifestData.plainMessage
                )
            }
            withContext(defaultDispatcher) {
                analysis.analyse()
                    .onSuccess { analysis ->
                        data.update { it.copy(txSummary = analysis.summary) }
                    }
                    .then { analysis ->
                        when (request.kind) {
                            is TransactionRequest.Kind.PreAuthorized -> processExpiration(request.kind.expiration)
                            is TransactionRequest.Kind.Regular -> fees.resolveFees(analysis)
                        }
                        if (!request.kind.isPreAuthorized) {
                            fees.resolveFees(analysis)
                        } else {
                            Result.success(Unit)
                        }
                    }
            }

            viewModelScope.launch(defaultDispatcher) {
                processDApp(request)
            }
        }
    }

    private suspend fun processDApp(request: TransactionRequest) {
        if (request.isInternal) {
            _state.update { it.copy(proposingDApp = State.ProposingDApp.None) }
        } else {
            getDAppsUseCase(AccountAddress.init(request.requestMetadata.dAppDefinitionAddress), false)
                .onSuccess { dApp ->
                    _state.update {
                        it.copy(proposingDApp = State.ProposingDApp.Some(dApp))
                    }
                }
        }
    }

    private fun processExpiration(expiration: SubintentExpiration) {
        when (expiration) {
            is SubintentExpiration.AtTime -> {
                var expirationDuration = expiration.expirationDuration()

                viewModelScope.launch {
                    do {
                        _state.update { it.copy(expiration = State.Expiration(duration = expirationDuration, startsAfterSign = false)) }
                        expirationDuration -= 1.seconds
                        delay(EXPIRATION_COUNTDOWN_PERIOD_MS)
                    } while (expirationDuration >= 0.seconds)
                }
            }

            is SubintentExpiration.DelayAfterSign -> {
                _state.update {
                    it.copy(
                        expiration = State.Expiration(
                            duration = expiration.delay,
                            startsAfterSign = true
                        )
                    )
                }
            }
        }
    }

    fun onBackClick() {
        if (state.value.sheetState != Sheet.None) {
            _state.update { it.copy(sheetState = Sheet.None) }
        } else {
            viewModelScope.launch {
                submit.onDismiss(exception = DappRequestException.RejectedByUser)
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
        viewModelScope.launch {
            val error = state.value.error?.error
            if (error is DappRequestException) {
                submit.onDismiss(error)
            } else {
                incomingRequestRepository.requestHandled(args.interactionId)
                sendEvent(Event.Dismiss)
            }
            _state.update { it.copy(error = null) }
        }
    }

    fun onAcknowledgeRawTransactionWarning() {
        _state.update { it.copy(showRawTransactionWarning = false) }
    }

    fun onFailedSigningRestart() {
        submit.onRestartSignAndSubmitTransaction()
    }

    fun onFailedSigningCancel() {
        submit.onSigningCanceled()
    }

    data class Data(
        private val txRequest: TransactionRequest? = null,
        private val txSummary: Summary? = null,
        val ephemeralNotaryPrivateKey: Curve25519SecretKey = Curve25519SecretKey.secureRandom(),
        val signers: List<ProfileEntity> = emptyList(),
        val latestFeesMode: Sheet.CustomizeFees.FeesMode = Sheet.CustomizeFees.FeesMode.Default,
        val feePayers: TransactionFeePayers? = null,
        val transactionFees: TransactionFees? = null
    ) {

        val request: TransactionRequest
            get() = requireNotNull(txRequest)

        val summary: Summary
            get() = requireNotNull(txSummary)
    }

    data class State(
        val isLoading: Boolean,
        val isPreAuthorization: Boolean = false,
        val proposingDApp: ProposingDApp? = null,
        val isRawManifestVisible: Boolean = false,
        val rawManifest: String = "",
        val showRawTransactionWarning: Boolean = false,
        val message: String? = null,
        val previewType: PreviewType,
        val sheetState: Sheet = Sheet.None,
        val fees: Fees? = null,
        val expiration: Expiration? = null,
        val error: TransactionErrorMessage? = null,
        val hiddenResourceIds: PersistentList<ResourceIdentifier> = persistentListOf(),
        val isSubmitting: Boolean = false
    ) : UiState {

        val isPreviewDisplayable: Boolean = previewType != PreviewType.None && previewType != PreviewType.UnacceptableManifest

        val rawManifestIsPreviewable: Boolean
            get() = previewType is PreviewType.Transaction

        val isSheetVisible: Boolean
            get() = sheetState != Sheet.None

        val showDottedLine: Boolean
            get() = when (previewType) {
                is PreviewType.Transaction -> {
                    previewType.from.isNotEmpty() && previewType.to.isNotEmpty()
                }

                else -> false
            }

        val showReceiptEdges: Boolean
            get() = !isPreAuthorization && isPreviewDisplayable

        data class Expiration(
            val duration: Duration,
            val startsAfterSign: Boolean
        ) {

            val isExpired: Boolean
                get() = duration == 0.seconds

            val truncateSeconds: Boolean
                get() = duration >= 60.seconds
        }

        val isSubmitEnabled: Boolean = if (isPreviewDisplayable) {
            when {
                isPreAuthorization -> expiration?.isExpired?.not() ?: false
                fees == null || fees.isNetworkFeeLoading -> false
                else -> {
                    val isFeePayerSelected = fees.properties.noFeePayerSelected.not()
                    val isBalanceSufficient = fees.properties.isBalanceInsufficientToPayTheFee.not()
                    isFeePayerSelected && isBalanceSufficient
                }
            }
        } else {
            false
        }

        val isSubmitVisible: Boolean = isPreviewDisplayable

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

        sealed interface ProposingDApp {

            val name: String?
                get() = when (this) {
                    is Some -> dApp?.name
                    None -> null
                }

            data object None : ProposingDApp

            data class Some(val dApp: DApp?) : ProposingDApp
        }

        sealed interface Sheet {

            val sheetHeightPercent: Float
                get() = when (this) {
                    is SigningFailed -> HEIGHT_80_PERCENT
                    else -> HEIGHT_90_PERCENT
                }

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

            data class SigningFailed(
                val type: Type
            ) : Sheet {

                enum class Type {
                    Transaction,
                    PreAuthorization
                }

                companion object {

                    fun from(isPreAuthorization: Boolean) = SigningFailed(type = if (isPreAuthorization) {
                        Type.PreAuthorization
                    } else {
                        Type.Transaction
                    })

                }

            }
        }

        data class SelectFeePayerInput(
            val preselectedCandidate: TransactionFeePayers.FeePayerCandidate?,
            val candidates: PersistentList<TransactionFeePayers.FeePayerCandidate>,
            val fee: String
        )

        companion object {
            private const val HEIGHT_80_PERCENT = 0.8f
            private const val HEIGHT_90_PERCENT = 0.9f
        }
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

    data class Transaction(
        val from: List<AccountWithTransferables>,
        val to: List<AccountWithTransferables>,
        val involvedComponents: InvolvedComponents,
        override val badges: List<Badge>,
        private val newlyCreatedGlobalIds: List<NonFungibleGlobalId> = emptyList()
    ) : PreviewType {

        val newlyCreatedResources: List<Resource>
            get() = (from + to).map { allTransfers ->
                allTransfers.transferables.filter { it.isNewlyCreated }.map { it.asset.resource }
            }.flatten()

        val newlyCreatedNFTs: List<Resource.NonFungibleResource.Item>
            get() {
                val allItems = (from + to).asSequence().map { it.transferables }.flatten().map {
                    when (it) {
                        is Transferable.NonFungibleType.NFTCollection -> it.amount.certain
                        is Transferable.NonFungibleType.StakeClaim -> it.amount.certain
                        else -> emptyList()
                    }
                }.flatten().associateBy { it.globalId }

                return newlyCreatedGlobalIds.mapNotNull { allItems[it] }
            }

        sealed interface InvolvedComponents {

            val isEmpty: Boolean
                get() = when (this) {
                    is None -> true
                    is DApps -> components.isEmpty() && !morePossibleDAppsPresent
                    is Pools -> pools.isEmpty()
                    is Validators -> validators.isEmpty()
                }

            data class DApps(
                val components: List<Pair<ManifestEncounteredComponentAddress, DApp?>>,
                val morePossibleDAppsPresent: Boolean = false
            ) : InvolvedComponents {

                val verifiedDapps: List<DApp>
                    get() = components.mapNotNull { it.second }

                val unknownComponents: List<ManifestEncounteredComponentAddress>
                    get() = components.mapNotNull { if (it.second == null) it.first else null }
            }

            data class Validators(
                val validators: Set<Validator>,
                val actionType: ActionType
            ) : InvolvedComponents {

                enum class ActionType {
                    Stake,
                    Unstake,
                    ClaimStake
                }
            }

            data class Pools(
                val pools: Set<Pool>,
                val actionType: ActionType
            ) : InvolvedComponents {

                val associatedDApps: List<DApp>
                    get() = pools.mapNotNull { it.associatedDApp }

                val unknownPools: List<Pool>
                    get() = pools.filter { it.associatedDApp == null }

                enum class ActionType {
                    Contribution,
                    Redemption
                }
            }

            data object None : InvolvedComponents
        }
    }

    data class DeleteAccount(
        val deletingAccount: Account,
        val to: AccountWithTransferables?
    ) : PreviewType {
        override val badges: List<Badge> = emptyList()
    }
}
