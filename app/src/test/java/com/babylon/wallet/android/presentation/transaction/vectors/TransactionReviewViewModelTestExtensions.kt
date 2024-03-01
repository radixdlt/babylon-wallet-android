package com.babylon.wallet.android.presentation.transaction.vectors

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.ResolveComponentAddressesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveNotaryAndSignersUseCase
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.SignTransactionUseCase
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModelTestExperimental
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
import kotlinx.coroutines.test.TestScope
import rdx.works.core.domain.DApp
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.ret.transaction.TransactionManifestData

internal fun TransactionReviewViewModelTestExperimental.testViewModel(
    transactionRepository: TransactionRepository,
    incomingRequestRepository: IncomingRequestRepository,
    signTransactionUseCase: SignTransactionUseCase,
    profileRepository: ProfileRepository,
    stateRepository: StateRepository,
    dAppMessenger: DappMessenger,
    appEventBus: AppEventBus,
    exceptionMessageProvider: ExceptionMessageProvider,
    savedStateHandle: SavedStateHandle,
    testScope: TestScope
) = TransactionReviewViewModel(
    signTransactionUseCase = signTransactionUseCase,
    analysis = TransactionAnalysisDelegate(
        previewTypeAnalyzer = PreviewTypeAnalyzer(
            generalTransferProcessor = GeneralTransferProcessor(
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository),
                getTransactionBadgesUseCase = GetTransactionBadgesUseCase(stateRepository),
                getProfileUseCase = GetProfileUseCase(profileRepository),
                resolveComponentAddressesUseCase = ResolveComponentAddressesUseCase(stateRepository)
            ),
            transferProcessor = TransferProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository)
            ),
            poolRedemptionProcessor = PoolRedemptionProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository)
            ),
            poolContributionProcessor = PoolContributionProcessor(
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository),
                getProfileUseCase = GetProfileUseCase(profileRepository)
            ),
            accountDepositSettingsProcessor = AccountDepositSettingsProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository),
            ),
            validatorClaimProcessor = ValidatorClaimProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository)
            ),
            validatorUnstakeProcessor = ValidatorUnstakeProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository)
            ),
            validatorStakeProcessor = ValidatorStakeProcessor(
                getProfileUseCase = GetProfileUseCase(profileRepository),
                resolveAssetsFromAddressUseCase = ResolveAssetsFromAddressUseCase(stateRepository)
            )
        ),
        cacheNewlyCreatedEntitiesUseCase = CacheNewlyCreatedEntitiesUseCase(stateRepository),
        resolveNotaryAndSignersUseCase = ResolveNotaryAndSignersUseCase(GetProfileUseCase(profileRepository)),
        searchFeePayersUseCase = SearchFeePayersUseCase(GetProfileUseCase(profileRepository), stateRepository),
        transactionRepository = transactionRepository
    ),
    guarantees = TransactionGuaranteesDelegate(),
    fees = TransactionFeesDelegate(getProfileUseCase = GetProfileUseCase(profileRepository)),
    submit = TransactionSubmitDelegate(
        dAppMessenger = dAppMessenger,
        getCurrentGatewayUseCase = GetCurrentGatewayUseCase(profileRepository),
        incomingRequestRepository = incomingRequestRepository,
        submitTransactionUseCase = SubmitTransactionUseCase(transactionRepository = transactionRepository),
        appEventBus = appEventBus,
        transactionStatusClient = TransactionStatusClient(PollTransactionStatusUseCase(transactionRepository), appEventBus, testScope),
        exceptionMessageProvider = exceptionMessageProvider,
        applicationScope = testScope
    ),
    getDAppsUseCase = GetDAppsUseCase(stateRepository),
    incomingRequestRepository = incomingRequestRepository,
    savedStateHandle = savedStateHandle
)

internal fun TransactionReviewViewModelTestExperimental.sampleManifest(
    instructions: String,
    networkId: Int = Radix.Network.mainnet.id,
    message: String? = null
) = TransactionManifestData(
    instructions = instructions,
    networkId = networkId,
    message = if (message == null) TransactionManifestData.TransactionMessage.None else TransactionManifestData.TransactionMessage.Public(message)
)

internal fun TransactionReviewViewModelTestExperimental.requestMetadata(
    manifestData: TransactionManifestData,
    dApp: DApp? = null
) = MessageFromDataChannel.IncomingRequest.RequestMetadata(
    networkId = manifestData.networkId,
    origin = dApp?.claimedWebsites?.firstOrNull().orEmpty(),
    dAppDefinitionAddress = dApp?.dAppAddress.orEmpty(),
    isInternal = dApp == null,
)