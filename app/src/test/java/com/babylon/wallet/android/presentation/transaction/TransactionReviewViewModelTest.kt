package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.gateway.generated.models.CoreApiTransactionReceipt
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.transaction.NotarizedTransactionResult
import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Badge
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetValidatorsUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppInTransactionUseCase
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetNFTDetailsUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.transaction.analysis.TransactionAnalysisDelegate
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFeesDelegate
import com.babylon.wallet.android.presentation.transaction.guarantees.TransactionGuaranteesDelegate
import com.babylon.wallet.android.presentation.transaction.submit.TransactionSubmitDelegate
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.FeeLocks
import com.radixdlt.ret.FeeSummary
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestClass
import com.radixdlt.ret.ManifestSummary
import com.radixdlt.ret.NewEntities
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.ret.TransactionManifest
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.mockito.kotlin.mock
import rdx.works.core.displayableQuantity
import rdx.works.core.identifiedArrayListOf
import rdx.works.core.ret.crypto.PrivateKey
import rdx.works.core.toIdentifiedArrayList
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionReviewViewModelTest : StateViewModelTest<TransactionReviewViewModel>() {

    @get:Rule
    val defaultLocaleTestRule = DefaultLocaleRule()

    private val transactionClient = mockk<TransactionClient>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getResourcesUseCase = mockk<GetResourcesUseCase>()
    private val cacheNewlyCreatedEntitiesUseCase = mockk<CacheNewlyCreatedEntitiesUseCase>()
    private val searchFeePayersUseCase = mockk<SearchFeePayersUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getTransactionBadgesUseCase = mockk<GetTransactionBadgesUseCase>()
    private val submitTransactionUseCase = mockk<SubmitTransactionUseCase>()
    private val transactionStatusClient = mockk<TransactionStatusClient>()
    private val resolveDappsInTransactionUseCase = mockk<ResolveDAppInTransactionUseCase>()
    private val getNFTDetailsUseCase = mockk<GetNFTDetailsUseCase>()
    private val incomingRequestRepository = IncomingRequestRepositoryImpl()
    private val dAppMessenger = mockk<DappMessenger>()
    private val appEventBus = mockk<AppEventBus>()
    private val deviceCapabilityHelper = mockk<DeviceCapabilityHelper>()
    private val getValidatorsUseCase = mockk<GetValidatorsUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val exceptionMessageProvider = mockk<ExceptionMessageProvider>()
    private val getDAppsUseCase = mockk<GetDAppsUseCase>()
    private val sampleTxId = "txId1"
    private val sampleRequestId = "requestId1"
    private val sampleRequest = mockk<MessageFromDataChannel.IncomingRequest.TransactionRequest>().apply {
        every { remoteConnectorId } returns "remoteConnectorId"
        every { requestId } returns sampleRequestId
        every { blockUntilComplete } returns false
        every { transactionType } returns com.babylon.wallet.android.data.dapp.model.TransactionType.Generic
        every { requestMetadata } returns MessageFromDataChannel.IncomingRequest.RequestMetadata(
            networkId = Radix.Gateway.nebunet.network.id,
            origin = "https://test.origin.com",
            dAppDefinitionAddress = "account_tdx_b_1p95nal0nmrqyl5r4phcspg8ahwnamaduzdd3kaklw3vqeavrwa",
            isInternal = false
        )
    }
    private val sampleTransactionManifestData = mockk<TransactionManifestData>().apply {
        every { networkId } returns Radix.Gateway.nebunet.network.id
        every { message } returns ""
    }
    private val manifest = mockk<TransactionManifest>().apply {
        every { instructions() } returns mockk<Instructions>().apply { every { asStr() } returns "" }
        every { blobs() } returns listOf()
    }
    private val fromAccount = account(
        address = "account_tdx_19jd32jd3928jd3892jd329",
        name = "From Account"
    )
    private val otherAccounts = identifiedArrayListOf(
        account(
            address = "account_tdx_3j892dj3289dj32d2d2d2d9",
            name = "To Account 1"
        ),
        account(
            address = "account_tdx_39jfc32jd932ke9023j89r9",
            name = "To Account 2"
        ),
        account(
            address = "account_tdx_12901829jd9281jd189jd98",
            name = "To account 3"
        )
    )

    private val emptyExecutionSummary = ExecutionSummary(
        feeLocks = FeeLocks(
            lock = Decimal.zero(),
            contingentLock = Decimal.zero()
        ),
        feeSummary = FeeSummary(
            executionCost = Decimal.zero(),
            finalizationCost = Decimal.zero(),
            storageExpansionCost = Decimal.zero(),
            royaltyCost = Decimal.zero()
        ),
        detailedClassification = listOf(),
        reservedInstructions = listOf(),
        accountDeposits = mapOf(),
        accountsRequiringAuth = listOf(),
        accountWithdraws = mapOf(),
        encounteredEntities = listOf(),
        identitiesRequiringAuth = listOf(),
        newEntities = NewEntities(
            componentAddresses = listOf(),
            resourceAddresses = listOf(),
            packageAddresses = listOf(),
            metadata = mapOf()
        ),
        presentedProofs = listOf()
    )

    @Before
    override fun setUp() = runTest {
        super.setUp()
        coEvery {
            getDAppsUseCase("account_tdx_b_1p95nal0nmrqyl5r4phcspg8ahwnamaduzdd3kaklw3vqeavrwa", false)
        } returns Result.success(DApp("account_tdx_b_1p95nal0nmrqyl5r4phcspg8ahwnamaduzdd3kaklw3vqeavrwa"))
        coEvery { getValidatorsUseCase(any()) } returns Result.success(listOf(ValidatorDetail("addr", BigDecimal(100000))))
        every { exceptionMessageProvider.throwableMessage(any()) } returns ""
        every { deviceCapabilityHelper.isDeviceSecure() } returns true
        every { savedStateHandle.get<String>(ARG_TRANSACTION_REQUEST_ID) } returns sampleRequestId
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.nebunet
        coEvery { submitTransactionUseCase(any(), any(), any()) } returns Result.success(
            SubmitTransactionUseCase.SubmitTransactionResult(sampleTxId, 50u)
        )
        coEvery { getNFTDetailsUseCase(any(), any()) } returns Result.success(emptyList())
        coEvery { getTransactionBadgesUseCase.invoke(any()) } returns listOf(
            Badge(address = "")
        )
        coEvery { transactionClient.signTransaction(any(), any(), any(), any()) } returns Result.success(
            NotarizedTransactionResult(
                "sampleTxId", "", TransactionHeader(
                    11u,
                    1U,
                    5U,
                    10u,
                    com.radixdlt.ret.PublicKey.Ed25519(byteArrayOf()),
                    false,
                    10u
                )
            )
        )
        coEvery { transactionClient.getNotaryAndSigners(any(), any()) } returns NotaryAndSigners(
            emptyList(),
            PrivateKey.EddsaEd25519.newRandom()
        )
        coEvery { searchFeePayersUseCase(any(), any()) } returns Result.success(FeePayerSearchResult("feePayer"))
        coEvery { transactionClient.signingState } returns emptyFlow()
        coEvery { transactionClient.getTransactionPreview(any(), any()) } returns Result.success(
            previewResponse()
        )
        coEvery { transactionStatusClient.pollTransactionStatus(any(), any(), any(), any()) } just Runs
        coEvery {
            dAppMessenger.sendTransactionWriteResponseSuccess(
                remoteConnectorId = "remoteConnectorId",
                requestId = sampleRequestId,
                txId = sampleTxId
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
        every { sampleRequest.isInternal } returns false
        every { sampleRequest.id } returns sampleRequestId
        every { sampleRequest.transactionManifestData } returns sampleTransactionManifestData
        incomingRequestRepository.add(sampleRequest)
        every { sampleTransactionManifestData.toTransactionManifest() } returns Result.success(manifest)
        every { manifest.executionSummary(any(), any()) } returns emptyExecutionSummary
        every { manifest.summary(any()) } returns ManifestSummary(
            accountsDepositedInto = emptyList(),
            accountsRequiringAuth = emptyList(),
            accountsWithdrawnFrom = emptyList(),
            presentedProofs = emptyList(),
            identitiesRequiringAuth = emptyList(),
            encounteredEntities = emptyList(),
            classification = listOf(ManifestClass.GENERAL),
            reservedInstructions = emptyList(),
        )
        every { getProfileUseCase() } returns flowOf(profile(accounts = (identifiedArrayListOf(fromAccount) + otherAccounts).toIdentifiedArrayList()))
        coEvery { getResourcesUseCase(any()) } returns Result.success(listOf())
    }

    override fun initVM(): TransactionReviewViewModel {
        return TransactionReviewViewModel(
            transactionClient = transactionClient,
            analysis = TransactionAnalysisDelegate(
                getProfileUseCase = getProfileUseCase,
                getResourcesUseCase = getResourcesUseCase,
                cacheNewlyCreatedEntitiesUseCase = cacheNewlyCreatedEntitiesUseCase,
                getTransactionBadgesUseCase = getTransactionBadgesUseCase,
                resolveDAppInTransactionUseCase = resolveDappsInTransactionUseCase,
                searchFeePayersUseCase = searchFeePayersUseCase,
                getValidatorsUseCase = getValidatorsUseCase,
                getNFTDetailsUseCase = getNFTDetailsUseCase
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
                txId = sampleTxId
            )
        }
    }

    @Test
    fun `transaction approval wrong network`() = runTest {
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.hammunet
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
        coEvery { transactionClient.signTransaction(any(), any(), any(), any()) } returns Result.failure(
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
    fun `given all fees are zero, network royalty and total fee are 0 (none due)`() = runTest {
        every { manifest.executionSummary(any(), any()) } returns emptyExecutionSummary.copy(
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )
        val vm = vm.value
        advanceUntilIdle()
        assertEquals("0.1083795", vm.state.value.transactionFees.networkFeeDisplayed)
        assertEquals("0", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.1083795", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 1`() = runTest {
        every { manifest.executionSummary(any(), any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("0.9"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )
        val vm = vm.value
        advanceUntilIdle()
        assertEquals("0.1283795", vm.state.value.transactionFees.networkFeeDisplayed)
        assertEquals("0.2", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.3283795", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 2`() = runTest {
        every { manifest.executionSummary(any(), any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("0.5"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )
        val vm = vm.value
        advanceUntilIdle()
        assertEquals("0.5283795", vm.state.value.transactionFees.networkFeeDisplayed)
        assertEquals("0.2", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.7283795", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 3`() = runTest {
        every { manifest.executionSummary(any(), any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("1.0"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )
        val vm = vm.value
        advanceUntilIdle()
        assertEquals("0.0283795", vm.state.value.transactionFees.networkFeeDisplayed)
        assertEquals("0.2", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.2283795", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 4`() = runTest {
        every { manifest.executionSummary(any(), any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("1.5"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )
        val vm = vm.value
        advanceUntilIdle()
        assertNull(vm.state.value.transactionFees.networkFeeDisplayed)
        assertEquals("0", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("0", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 4 with one signer account`() = runTest {
        every { manifest.executionSummary(any(), any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal.zero(),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        coEvery { transactionClient.getNotaryAndSigners(any(), any()) } returns NotaryAndSigners(
            listOf(
                SampleDataProvider().sampleAccount(
                    address = "rdx_t_12382918379821",
                    name = "Savings account"
                )
            ),
            PrivateKey.EddsaEd25519.newRandom()
        )

        val vm = vm.value
        advanceUntilIdle()
        assertEquals("1.040813", vm.state.value.transactionFees.networkFeeDisplayed)//0.9265478
        assertEquals("0.2", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("1.240813", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())//1.26553
    }

    @Test
    fun `verify transaction fee to lock is correct on advanced screen 1`() = runTest {
        val feePaddingAmount = "1.6"

        // Sum of executionCost finalizationCost storageExpansionCost royaltyCost padding and tip minus noncontingentlock
        val expectedFeeLock = "1.10842739440"
        every { manifest.executionSummary(any(), any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("1.5"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.onFeePaddingAmountChanged(feePaddingAmount)

        assertEquals(BigDecimal(expectedFeeLock), vm.state.value.transactionFees.transactionFeeToLock)
    }

    @Test
    fun `verify transaction fee to lock is correct on advanced screen 2`() = runTest {
        val tipPercentage = "25"

        // Sum of executionCost finalizationCost royaltyCost padding and tip minus noncontingentlock
        val expectedFeeLock = "0.9019403"

        every { manifest.executionSummary(any(), any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("0.5"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.onTipPercentageChanged(tipPercentage)

        assertEquals(expectedFeeLock, vm.state.value.transactionFees.transactionFeeToLock.displayableQuantity())
    }

    @Test
    fun `verify transaction fee to lock is correct on advanced screen 3`() = runTest {
        val feePaddingAmount = "1.6"
        val tipPercentage = "25"

        // Sum of executionCost finalizationCost royaltyCost padding and tip minus noncontingentlock
        val expectedFeeLock = "2.3678038"

        every { manifest.executionSummary(any(), any()) } returns emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("0.5"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
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

        assertEquals(expectedFeeLock, vm.state.value.transactionFees.transactionFeeToLock.displayableQuantity())
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

class DefaultLocaleRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                Locale.setDefault(Locale.UK)
                base.evaluate()
            }
        }
    }
}
