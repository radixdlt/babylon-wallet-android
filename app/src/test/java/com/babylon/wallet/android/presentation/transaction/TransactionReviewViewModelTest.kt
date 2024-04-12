package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.DefaultLocaleRule
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.gateway.coreapi.CoreApiTransactionReceipt
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.data.transaction.model.TransactionFeePayers
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveComponentAddressesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveNotaryAndSignersUseCase
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.SignTransactionUseCase
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.presentation.StateViewModelTest
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
import com.babylon.wallet.android.utils.AppEventBusImpl
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.CompiledNotarizedIntent
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.FeeLocks
import com.radixdlt.sargon.FeeSummary
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NewEntities
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.rounded
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import rdx.works.core.domain.DApp
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.transaction.NotarizationResult
import rdx.works.core.logNonFatalException
import rdx.works.core.sargon.changeDefaultDepositGuarantee
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase

@Ignore("TODO Integration")
@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionReviewViewModelTest : StateViewModelTest<TransactionReviewViewModel>() {

    @get:Rule
    val defaultLocaleTestRule = DefaultLocaleRule()

    private val signTransactionUseCase = mockk<SignTransactionUseCase>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getResourcesUseCase = mockk<GetResourcesUseCase>()
    private val resolveAssetsFromAddressUseCase = mockk<ResolveAssetsFromAddressUseCase>()
    private val cacheNewlyCreatedEntitiesUseCase = mockk<CacheNewlyCreatedEntitiesUseCase>()
    private val searchFeePayersUseCase = mockk<SearchFeePayersUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getTransactionBadgesUseCase = mockk<GetTransactionBadgesUseCase>()
    private val submitTransactionUseCase = mockk<SubmitTransactionUseCase>()
    private val transactionStatusClient = mockk<TransactionStatusClient>()
    private val resolveNotaryAndSignersUseCase = mockk<ResolveNotaryAndSignersUseCase>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val incomingRequestRepository = IncomingRequestRepositoryImpl()
    private val appEventBus = mockk<AppEventBusImpl>()
    private val respondToIncomingRequestUseCase = mockk<RespondToIncomingRequestUseCase>()
    private val deviceCapabilityHelper = mockk<DeviceCapabilityHelper>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val exceptionMessageProvider = mockk<ExceptionMessageProvider>()
    private val getDAppsUseCase = mockk<GetDAppsUseCase>()
    private val resolveComponentAddressesUseCase = mockk<ResolveComponentAddressesUseCase>()
    private val previewTypeAnalyzer = PreviewTypeAnalyzer(
        generalTransferProcessor = GeneralTransferProcessor(
            resolveAssetsFromAddressUseCase = resolveAssetsFromAddressUseCase,
            getTransactionBadgesUseCase = getTransactionBadgesUseCase,
            getProfileUseCase = getProfileUseCase,
            resolveComponentAddressesUseCase = resolveComponentAddressesUseCase
        ),
        transferProcessor = TransferProcessor(
            getProfileUseCase = getProfileUseCase,
            resolveAssetsFromAddressUseCase = resolveAssetsFromAddressUseCase
        ),
        poolContributionProcessor = PoolContributionProcessor(
            getProfileUseCase = getProfileUseCase,
            resolveAssetsFromAddressUseCase = resolveAssetsFromAddressUseCase
        ),
        accountDepositSettingsProcessor = AccountDepositSettingsProcessor(
            getProfileUseCase = getProfileUseCase,
            resolveAssetsFromAddressUseCase = resolveAssetsFromAddressUseCase
        ),
        poolRedemptionProcessor = PoolRedemptionProcessor(
            getProfileUseCase = getProfileUseCase,
            resolveAssetsFromAddressUseCase = resolveAssetsFromAddressUseCase
        ),
        validatorStakeProcessor = ValidatorStakeProcessor(
            getProfileUseCase = getProfileUseCase,
            resolveAssetsFromAddressUseCase = resolveAssetsFromAddressUseCase
        ),
        validatorUnstakeProcessor = ValidatorUnstakeProcessor(
            getProfileUseCase = getProfileUseCase,
            resolveAssetsFromAddressUseCase = resolveAssetsFromAddressUseCase
        ),
        validatorClaimProcessor = ValidatorClaimProcessor(
            getProfileUseCase = getProfileUseCase,
            resolveAssetsFromAddressUseCase = resolveAssetsFromAddressUseCase
        )
    )
    private val sampleIntentHash = IntentHash.sample()
    private val notarizationResult = NotarizationResult(
        intentHash = sampleIntentHash,
        compiledNotarizedIntent = CompiledNotarizedIntent.sample(),
        endEpoch = 50u
    )
    private val sampleRequestId = "requestId1"
    private val sampleTransactionManifestData = mockk<TransactionManifestData>().apply {
        every { networkId } returns NetworkId.MAINNET
        every { instructions } returns ""
        every { blobs } returns emptyList()
        every { message } returns TransactionManifestData.TransactionMessage.None
        every { entitiesRequiringAuth() } returns TransactionManifestData.EntitiesRequiringAuth(
            accounts = emptyList(),
            identities = emptyList()
        )
    }
    private val sampleRequest = IncomingMessage.IncomingRequest.TransactionRequest(
        remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = sampleRequestId,
        transactionManifestData = sampleTransactionManifestData,
        requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata(
            networkId = NetworkId.MAINNET.discriminant.toInt(),
            origin = "https://test.origin.com",
            dAppDefinitionAddress = DApp.sampleMainnet().dAppAddress.string,
            isInternal = false
        ),
        transactionType = com.babylon.wallet.android.data.dapp.model.TransactionType.Generic
    )

    private val profile = Profile.sample()
        .changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities()
        .changeDefaultDepositGuarantee(defaultDepositGuarantee = 0.99.toDecimal192())

    private val emptyExecutionSummary = ExecutionSummary(
        feeLocks = FeeLocks(
            lock = 0.toDecimal192(),
            contingentLock = 0.toDecimal192()
        ),
        feeSummary = FeeSummary(
            executionCost = 0.toDecimal192(),
            finalizationCost = 0.toDecimal192(),
            storageExpansionCost = 0.toDecimal192(),
            royaltyCost = 0.toDecimal192()
        ),
        detailedClassification = listOf(),
        reservedInstructions = listOf(),
        deposits = mapOf(),
        withdrawals = mapOf(),
        addressesOfAccountsRequiringAuth = listOf(),
        addressesOfIdentitiesRequiringAuth = listOf(),
        encounteredComponentAddresses = listOf(),
        newEntities = NewEntities(
            metadata = mapOf()
        ),
        presentedProofs = listOf(),
        newlyCreatedNonFungibles = listOf()
    )

    @Before
    override fun setUp() = runTest {
        super.setUp()
        val dApp = DApp.sampleMainnet()
        coEvery {
            getDAppsUseCase(dApp.dAppAddress, false)
        } returns Result.success(dApp)
        every { exceptionMessageProvider.throwableMessage(any()) } returns ""
        every { deviceCapabilityHelper.isDeviceSecure() } returns true
        mockkStatic("rdx.works.core.CrashlyticsExtensionsKt")
        every { logNonFatalException(any()) } just Runs
        every { savedStateHandle.get<String>(ARG_TRANSACTION_REQUEST_ID) } returns sampleRequestId
        coEvery { getCurrentGatewayUseCase() } returns Gateway.forNetwork(NetworkId.MAINNET)
        coEvery { submitTransactionUseCase(any()) } returns Result.success(notarizationResult)
        coEvery { getTransactionBadgesUseCase(any()) } returns Result.success(listOf(
            Badge(address = ResourceAddress.sampleMainnet())
        ))
        coEvery { signTransactionUseCase.sign(any(), any()) } returns Result.success(notarizationResult)
        coEvery { signTransactionUseCase.signingState } returns emptyFlow()
        coEvery { searchFeePayersUseCase(any(), any()) } returns Result.success(TransactionFeePayers(AccountAddress.sampleMainnet.random()))
        coEvery { transactionRepository.getLedgerEpoch() } returns Result.success(0.toULong())
        coEvery { transactionRepository.getTransactionPreview(any()) } returns Result.success(previewResponse())
        coEvery { transactionStatusClient.pollTransactionStatus(any(), any(), any(), any()) } just Runs
        coEvery {
            dAppMessenger.sendTransactionWriteResponseSuccess(
                remoteConnectorId = "remoteConnectorId",
                requestId = sampleRequestId,
                txId = sampleIntentHash.bech32EncodedTxId
            )
        } returns Result.success(Unit)
        coEvery {
            dAppMessenger.sendWalletInteractionResponseFailure(
                remoteConnectorId = "remoteConnectorId",
                requestId = sampleRequestId,
                error = any(),
                message = any()
            )
        } returns Result.success(Unit)
        coEvery { appEventBus.sendEvent(any()) } returns Unit
        incomingRequestRepository.add(sampleRequest)
        every { getProfileUseCase.flow } returns flowOf(profile)
        coEvery { resolveNotaryAndSignersUseCase(any(), any(), any()) } returns Result.success(
            NotaryAndSigners(
                listOf(),
                Curve25519SecretKey.secureRandom()
            )
        )
        every { sampleTransactionManifestData.executionSummary(any()) } returns emptyExecutionSummary
        coEvery { getResourcesUseCase(any(), any()) } returns Result.success(listOf())
        coEvery { resolveAssetsFromAddressUseCase(any(), any()) } returns Result.success(listOf())
    }

    override fun initVM(): TransactionReviewViewModel {
        return TransactionReviewViewModel(
            signTransactionUseCase = signTransactionUseCase,
            analysis = TransactionAnalysisDelegate(
                previewTypeAnalyzer = previewTypeAnalyzer,
                cacheNewlyCreatedEntitiesUseCase = cacheNewlyCreatedEntitiesUseCase,
                searchFeePayersUseCase = searchFeePayersUseCase,
                resolveNotaryAndSignersUseCase = resolveNotaryAndSignersUseCase,
                transactionRepository = transactionRepository
            ),
            guarantees = TransactionGuaranteesDelegate(),
            fees = TransactionFeesDelegate(
                getProfileUseCase = getProfileUseCase,
            ),
            submit = TransactionSubmitDelegate(
                dAppMessenger = dAppMessenger,
                getCurrentGatewayUseCase = getCurrentGatewayUseCase,
                incomingRequestRepository = incomingRequestRepository,
                appEventBus = appEventBus,
                transactionStatusClient = transactionStatusClient,
                submitTransactionUseCase = submitTransactionUseCase,
                applicationScope = TestScope(),
                exceptionMessageProvider = exceptionMessageProvider
            ),
            incomingRequestRepository = incomingRequestRepository,
            savedStateHandle = savedStateHandle,
            getDAppsUseCase = getDAppsUseCase
        )
    }

    @Test
    fun `transaction approval success`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.approveTransaction { true }
        advanceUntilIdle()
        coVerify(exactly = 1) {
            dAppMessenger.sendTransactionWriteResponseSuccess(
                remoteConnectorId = "remoteConnectorId",
                requestId = sampleRequestId,
                txId = sampleIntentHash.bech32EncodedTxId
            )
        }
    }

    @Test
    fun `transaction approval wrong network`() = runTest {
        coEvery { getCurrentGatewayUseCase() } returns Gateway.forNetwork(NetworkId.STOKENET)
        val vm = vm.value
        advanceUntilIdle()
        vm.approveTransaction { true }
        advanceUntilIdle()
        val errorSlot = slot<WalletErrorType>()
        coVerify(exactly = 1) {
            dAppMessenger.sendWalletInteractionResponseFailure(
                remoteConnectorId = "remoteConnectorId",
                requestId = sampleRequestId,
                error = capture(errorSlot),
                message = any()
            )
        }
        assertEquals(WalletErrorType.WrongNetwork, errorSlot.captured)
        assertTrue(vm.state.value.isTransactionDismissed)
    }

    @Test
    fun `transaction approval sign and submit error`() = runTest {
        coEvery { signTransactionUseCase.sign(any(), any()) } returns Result.failure(
            RadixWalletException.PrepareTransactionException.SubmitNotarizedTransaction()
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.approveTransaction { true }
        advanceUntilIdle()
        val state = vm.state.first()
        val errorSlot = slot<WalletErrorType>()
        coVerify(exactly = 1) {
            dAppMessenger.sendWalletInteractionResponseFailure(
                remoteConnectorId = "remoteConnectorId",
                requestId = sampleRequestId,
                error = capture(errorSlot),
                message = any()
            )
        }
        assertEquals(WalletErrorType.FailedToSubmitTransaction, errorSlot.captured)
        assertNotNull(state.error)
    }

    @Test
    fun `verify transaction fee to lock is correct on advanced screen 1`() = runTest {
        val feePaddingAmount = "1.6"

        // Sum of executionCost finalizationCost storageExpansionCost royaltyCost padding and tip minus noncontingentlock
        val expectedFeeLock = "1.10842739440"
        every { sampleTransactionManifestData.executionSummary(any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = 1.5.toDecimal192(),
                contingentLock = 0.toDecimal192()
            ),
            feeSummary = FeeSummary(
                executionCost = 0.3.toDecimal192(),
                finalizationCost = 0.3.toDecimal192(),
                storageExpansionCost = 0.2.toDecimal192(),
                royaltyCost = 0.2.toDecimal192()
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.onFeePaddingAmountChanged(feePaddingAmount)

        assertEquals(expectedFeeLock.toDecimal192(), vm.state.value.transactionFees.transactionFeeToLock)
    }

    @Test
    fun `verify transaction fee to lock is correct on advanced screen 2`() = runTest {
        val tipPercentage = "25"

        // Sum of executionCost finalizationCost royaltyCost padding and tip minus noncontingentlock
        val expectedFeeLock = 0.9019403.toDecimal192()

        every { sampleTransactionManifestData.executionSummary(any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = 0.5.toDecimal192(),
                contingentLock = 0.toDecimal192()
            ),
            feeSummary = FeeSummary(
                executionCost = 0.3.toDecimal192(),
                finalizationCost = 0.3.toDecimal192(),
                storageExpansionCost = 0.2.toDecimal192(),
                royaltyCost = 0.2.toDecimal192()
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.onTipPercentageChanged(tipPercentage)

        assertEquals(expectedFeeLock, vm.state.value.transactionFees.transactionFeeToLock.rounded(7u))
    }

    @Test
    fun `verify transaction fee to lock is correct on advanced screen 3`() = runTest {
        val feePaddingAmount = "1.6"
        val tipPercentage = "25"

        // Sum of executionCost finalizationCost royaltyCost padding and tip minus noncontingentlock
        val expectedFeeLock = 2.3678038.toDecimal192()

        every { sampleTransactionManifestData.executionSummary(any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = 0.5.toDecimal192(),
                contingentLock = 0.toDecimal192()
            ),
            feeSummary = FeeSummary(
                executionCost = 0.3.toDecimal192(),
                finalizationCost = 0.3.toDecimal192(),
                storageExpansionCost = 0.2.toDecimal192(),
                royaltyCost = 0.2.toDecimal192()
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.onFeePaddingAmountChanged(feePaddingAmount)
        vm.onTipPercentageChanged(tipPercentage)

        assertEquals(expectedFeeLock, vm.state.value.transactionFees.transactionFeeToLock.rounded(7u))
    }

    private fun previewResponse() = TransactionPreviewResponse(
        encodedReceipt = "",
        receipt = CoreApiTransactionReceipt(
            status = "",
            errorMessage = ""
        ),
        logs = emptyList()
    )
}