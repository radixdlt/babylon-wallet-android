package com.babylon.wallet.android.presentation.transaction.vectors

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.ResolveComponentAddressesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveNotaryAndSignersUseCase
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.ClearCachedNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.domain.usecases.signing.SignTransactionUseCase
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.analysis.TransactionAnalysisDelegate
import com.babylon.wallet.android.presentation.transaction.analysis.processor.AccountDepositSettingsProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.GeneralTransferProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.PoolContributionProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.PoolRedemptionProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.PreviewTypeAnalyzer
import com.babylon.wallet.android.presentation.transaction.analysis.processor.TransferProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.ValidatorClaimProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.ValidatorStakeProcessor
import com.babylon.wallet.android.presentation.transaction.analysis.processor.ValidatorUnstakeProcessor
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFeesDelegate
import com.babylon.wallet.android.presentation.transaction.guarantees.TransactionGuaranteesDelegate
import com.babylon.wallet.android.presentation.transaction.submit.TransactionSubmitDelegate
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestScope
import rdx.works.core.domain.DApp
import rdx.works.core.domain.TransactionManifestData
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
        previewTypeAnalyzer = PreviewTypeAnalyzer(
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
        searchFeePayersUseCase = SearchFeePayersUseCase(GetProfileUseCase(profileRepository, testDispatcher), stateRepository),
        transactionRepository = transactionRepository,
        getFiatValueUseCase = getFiatValueUseCase
    ),
    guarantees = TransactionGuaranteesDelegate(),
    fees = TransactionFeesDelegate(getProfileUseCase = GetProfileUseCase(profileRepository, testDispatcher)),
    submit = TransactionSubmitDelegate(
        signTransactionUseCase = signTransactionUseCase,
        respondToIncomingRequestUseCase = respondToIncomingRequestUseCase,
        getCurrentGatewayUseCase = GetCurrentGatewayUseCase(profileRepository),
        incomingRequestRepository = incomingRequestRepository,
        submitTransactionUseCase = SubmitTransactionUseCase(transactionRepository = transactionRepository),
        clearCachedNewlyCreatedEntitiesUseCase = ClearCachedNewlyCreatedEntitiesUseCase(stateRepository = stateRepository),
        appEventBus = appEventBus,
        transactionStatusClient = TransactionStatusClient(
            pollTransactionStatusUseCase = PollTransactionStatusUseCase(transactionRepository = transactionRepository),
            appEventBus = appEventBus,
            preferencesManager = preferencesManager,
            appScope = testScope
        ),
        exceptionMessageProvider = exceptionMessageProvider,
        applicationScope = testScope
    ),
    getDAppsUseCase = GetDAppsUseCase(stateRepository),
    incomingRequestRepository = incomingRequestRepository,
    savedStateHandle = savedStateHandle,
    appEventBus = appEventBus,
    getProfileUseCase = getProfileUseCase,
    coroutineDispatcher = testDispatcher
)

internal fun sampleManifest(
    instructions: String,
    networkId: NetworkId = NetworkId.MAINNET,
    message: String? = null
) = TransactionManifestData(
    instructions = instructions,
    networkId = networkId,
    message = if (message == null) TransactionManifestData.TransactionMessage.None else TransactionManifestData.TransactionMessage.Public(message)
)

internal fun requestMetadata(
    manifestData: TransactionManifestData,
    dApp: DApp? = null
) = DappToWalletInteraction.RequestMetadata(
    networkId = manifestData.networkId,
    origin = dApp?.claimedWebsites?.firstOrNull().orEmpty(),
    dAppDefinitionAddress = dApp?.dAppAddress?.string.orEmpty(),
    isInternal = dApp == null,
)