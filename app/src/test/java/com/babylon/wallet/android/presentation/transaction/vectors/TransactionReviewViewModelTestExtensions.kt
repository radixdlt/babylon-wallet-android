package com.babylon.wallet.android.presentation.transaction.vectors

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.ResolveComponentAddressesUseCase
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.assets.ClearCachedNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.domain.usecases.signing.ResolveNotaryAndSignersUseCase
import com.babylon.wallet.android.domain.usecases.signing.SignTransactionUseCase
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.analysis.TransactionAnalysisDelegate
import com.babylon.wallet.android.presentation.transaction.analysis.processor.AccountDepositSettingsProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.GeneralTransferProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.PoolContributionProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.PoolRedemptionProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.ExecutionSummaryAnalyser
import com.babylon.wallet.android.presentation.transaction.analysis.processor.TransferProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.ValidatorClaimProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.ValidatorStakeProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.ValidatorUnstakeProcessor
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFeesDelegateImpl
import com.babylon.wallet.android.presentation.transaction.guarantees.TransactionGuaranteesDelegateImpl
import com.babylon.wallet.android.presentation.transaction.submit.TransactionSubmitDelegateImpl
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import com.radixdlt.sargon.Blobs
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toList
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestScope
import rdx.works.core.domain.DApp
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase

internal fun testViewModel(
    transactionRepository: TransactionRepository,
    incomingRequestRepository: IncomingRequestRepository,
    signTransactionUseCase: SignTransactionUseCase,
    profileRepository: ProfileRepository,
    stateRepository: StateRepository,
    respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    appEventBus: AppEventBus,
    preferencesManager: PreferencesManager,
    exceptionMessageProvider: ExceptionMessageProvider,
    getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle,
    testDispatcher: CoroutineDispatcher,
    testScope: TestScope,
    getFiatValueUseCase: GetFiatValueUseCase
) = TransactionReviewViewModel(
    analysis = TransactionAnalysisDelegate(
        executionSummaryAnalyser = ExecutionSummaryAnalyser(
            generalTransferProcessor = GeneralTransferProcessor(
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository),
                getProfileUseCase = GetProfileUseCase(profileRepository, testDispatcher),
                resolveComponentAddressesUseCase = ResolveComponentAddressesUseCase(stateRepository)
            ),
            transferProcessor = TransferProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository, testDispatcher),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository)
            ),
            poolRedemptionProcessor = PoolRedemptionProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository, testDispatcher),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository)
            ),
            poolContributionProcessor = PoolContributionProcessor(
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository),
                getProfileUseCase = GetProfileUseCase(profileRepository, testDispatcher)
            ),
            accountDepositSettingsProcessor = AccountDepositSettingsProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository, testDispatcher),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository),
            ),
            validatorClaimProcessor = ValidatorClaimProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository, testDispatcher),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository)
            ),
            validatorUnstakeProcessor = ValidatorUnstakeProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository, testDispatcher),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository)
            ),
            validatorStakeProcessor = ValidatorStakeProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository, testDispatcher),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository)
            )
        ),
        cacheNewlyCreatedEntitiesUseCase = CacheNewlyCreatedEntitiesUseCase(stateRepository),
        resolveNotaryAndSignersUseCase = ResolveNotaryAndSignersUseCase(GetProfileUseCase(profileRepository, testDispatcher)),
        transactionRepository = transactionRepository
    ),
    guarantees = TransactionGuaranteesDelegateImpl(),
    fees = TransactionFeesDelegateImpl(
        getProfileUseCase = GetProfileUseCase(profileRepository, testDispatcher),
        searchFeePayersUseCase = SearchFeePayersUseCase(GetProfileUseCase(profileRepository, testDispatcher), stateRepository),
        getFiatValueUseCase = getFiatValueUseCase
    ),
    submit = TransactionSubmitDelegateImpl(
        signTransactionUseCase = signTransactionUseCase,
        respondToIncomingRequestUseCase = respondToIncomingRequestUseCase,
        getCurrentGatewayUseCase = GetCurrentGatewayUseCase(profileRepository),
        incomingRequestRepository = incomingRequestRepository,
        clearCachedNewlyCreatedEntitiesUseCase = ClearCachedNewlyCreatedEntitiesUseCase(stateRepository = stateRepository),
        appEventBus = appEventBus,
        transactionStatusClient = TransactionStatusClient(
            pollTransactionStatusUseCase = PollTransactionStatusUseCase(transactionRepository = transactionRepository),
            appEventBus = appEventBus,
            preferencesManager = preferencesManager,
            appScope = testScope
        ),
        exceptionMessageProvider = exceptionMessageProvider,
        applicationScope = testScope,
        transactionRepository = transactionRepository
    ),
    getDAppsUseCase = GetDAppsUseCase(stateRepository),
    incomingRequestRepository = incomingRequestRepository,
    savedStateHandle = savedStateHandle,
    appEventBus = appEventBus,
    getProfileUseCase = getProfileUseCase,
    defaultDispatcher = testDispatcher
)

internal fun sampleManifest(
    instructions: String,
    networkId: NetworkId = NetworkId.MAINNET,
    message: String? = null
) = UnvalidatedManifestData(
    instructions = instructions,
    networkId = networkId,
    plainMessage = message,
    blobs = Blobs.sample().toList().map { it.bytes }
)

internal fun requestMetadata(
    manifestData: UnvalidatedManifestData,
    dApp: DApp? = null
) = DappToWalletInteraction.RequestMetadata(
    networkId = manifestData.networkId,
    origin = dApp?.claimedWebsites?.firstOrNull().orEmpty(),
    dAppDefinitionAddress = dApp?.dAppAddress?.string.orEmpty(),
    isInternal = dApp == null,
)