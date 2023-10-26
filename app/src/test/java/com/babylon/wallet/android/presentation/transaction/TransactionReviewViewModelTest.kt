package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionReceipt
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.NotarizedTransactionResult
import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.resources.Badge
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.usecases.GetAccountsWithAssetsUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesMetadataUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppsUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.ExecutionAnalysis
import com.radixdlt.ret.FeeLocks
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.ret.TransactionType
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import rdx.works.core.displayableQuantity
import rdx.works.core.ret.crypto.PrivateKey
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import java.math.BigDecimal
import java.util.Locale
import com.babylon.wallet.android.domain.common.Result as ResultInternal

@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionReviewViewModelTest : StateViewModelTest<TransactionReviewViewModel>() {

    @get:Rule
    val defaultLocaleTestRule = DefaultLocaleRule()

    private val transactionClient = mockk<TransactionClient>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getAccountsWithAssetsUseCase = mockk<GetAccountsWithAssetsUseCase>()
    private val getResourcesUseCase = mockk<GetResourcesUseCase>()
    private val getResourcesMetadataUseCase = mockk<GetResourcesMetadataUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getTransactionBadgesUseCase = mockk<GetTransactionBadgesUseCase>()
    private val submitTransactionUseCase = mockk<SubmitTransactionUseCase>()
    private val transactionStatusClient = mockk<TransactionStatusClient>()
    private val resolveDAppsUseCase = mockk<ResolveDAppsUseCase>()
    private val incomingRequestRepository = IncomingRequestRepositoryImpl()
    private val dAppMessenger = mockk<DappMessenger>()
    private val appEventBus = mockk<AppEventBus>()
    private val deviceSecurityHelper = mockk<DeviceSecurityHelper>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val sampleTxId = "txId1"
    private val sampleRequestId = "requestId1"
    private val sampleRequest = MessageFromDataChannel.IncomingRequest.TransactionRequest(
        remoteConnectorId = "remoteConnectorId",
        requestId = sampleRequestId,
        transactionManifestData = TransactionManifestData("", 1, 11),
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "https://test.origin.com",
            "account_tdx_b_1p95nal0nmrqyl5r4phcspg8ahwnamaduzdd3kaklw3vqeavrwa",
            false
        )
    )
    private val sampleProfile = profile(accounts = listOf(account(address = "adr_1", name = "primary")))
    private val fromAccount = account(
        address = "account_tdx_19jd32jd3928jd3892jd329",
        name = "From Account"
    )
    private val otherAccounts = listOf(
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
    private val sampleXrdResource = Resource.FungibleResource(
        resourceAddress = XrdResource.address(),
        ownedAmount = BigDecimal.TEN,
        symbolMetadataItem = SymbolMetadataItem(XrdResource.SYMBOL)
    )

    @Before
    override fun setUp() = runTest {
        super.setUp()
        every { deviceSecurityHelper.isDeviceSecure() } returns true
        every { savedStateHandle.get<String>(ARG_TRANSACTION_REQUEST_ID) } returns sampleRequestId
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.nebunet
        coEvery { submitTransactionUseCase(any(), any(), any()) } returns Result.success(
            SubmitTransactionUseCase.SubmitTransactionResult(sampleTxId, "50")
        )
        coEvery { getTransactionBadgesUseCase.invoke(any()) } returns listOf(
            Badge(address = "", nameMetadataItem = null, iconMetadataItem = null)
        )
        coEvery { transactionClient.signTransaction(any(), any(), any(), any()) } returns Result.success(
            NotarizedTransactionResult("sampleTxId", "", TransactionHeader(
                11u,
                1U,
                5U,
                10u,
                com.radixdlt.ret.PublicKey.Ed25519(emptyList()),
                false,
                10u
            ))
        )
        coEvery { transactionClient.getNotaryAndSigners(any(), any()) } returns NotaryAndSigners(emptyList(), PrivateKey.EddsaEd25519.newRandom())
        coEvery { transactionClient.findFeePayerInManifest(any(), any()) } returns Result.success(FeePayerSearchResult("feePayer"))
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
        } returns ResultInternal.Success(Unit)
        coEvery {
            dAppMessenger.sendWalletInteractionResponseFailure(
                remoteConnectorId = "remoteConnectorId",
                requestId = sampleRequestId,
                error = any(),
                message = any()
            )
        } returns ResultInternal.Success(Unit)
        incomingRequestRepository.add(sampleRequest)
        coEvery { appEventBus.sendEvent(any()) } returns Unit
        coEvery { transactionClient.analyzeExecution(any(), any()) } returns Result.success(
            ExecutionAnalysis(
                feeLocks = FeeLocks(
                    lock = Decimal.zero(),
                    contingentLock = Decimal.zero()
                ),
                feeSummary = com.radixdlt.ret.FeeSummary(
                    executionCost =  Decimal.zero(),
                    finalizationCost =  Decimal.zero(),
                    storageExpansionCost = Decimal.zero(),
                    royaltyCost = Decimal.zero()
                ),
                transactionTypes = listOf(),
                reservedInstructions = listOf()
            )
        )
        every { getProfileUseCase() } returns flowOf(profile(accounts = listOf(fromAccount) + otherAccounts))
        coEvery {
            getAccountsWithAssetsUseCase(
                accounts = any(),
                isRefreshing = false
            )
        } returns com.babylon.wallet.android.domain.common.Result.Success(
            listOf(
                AccountWithAssets(
                    account = sampleProfile.currentNetwork.accounts[0],
                    assets = Assets(
                        fungibles = listOf(sampleXrdResource),
                        nonFungibles = emptyList(),
                        poolUnits = emptyList()
                    )
                ),
                AccountWithAssets(
                    account = sampleProfile.currentNetwork.accounts[0],
                    assets = Assets(
                        fungibles = emptyList(),
                        nonFungibles = emptyList()
                    )
                )
            )
        )
        coEvery {
            getResourcesMetadataUseCase.invoke(
                resourceAddresses = any(),
                isRefreshing = false
            )
        } returns com.babylon.wallet.android.domain.common.Result.Success(
            mapOf()
        )
    }

    override fun initVM(): TransactionReviewViewModel {
        return TransactionReviewViewModel(
            transactionClient = transactionClient,
            getAccountsWithAssetsUseCase = getAccountsWithAssetsUseCase,
            getResourcesMetadataUseCase = getResourcesMetadataUseCase,
            getProfileUseCase = getProfileUseCase,
            getTransactionBadgesUseCase = getTransactionBadgesUseCase,
            resolveDAppsUseCase = resolveDAppsUseCase,
            getCurrentGatewayUseCase = getCurrentGatewayUseCase,
            dAppMessenger = dAppMessenger,
            appEventBus = appEventBus,
            incomingRequestRepository = incomingRequestRepository,
            appScope = TestScope(),
            savedStateHandle = savedStateHandle,
            transactionStatusClient = transactionStatusClient,
            getResourcesUseCase = getResourcesUseCase,
            submitTransactionUseCase = submitTransactionUseCase
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
        assertEquals(TransactionReviewViewModel.Event.Dismiss, vm.oneOffEvent.first())
    }

    @Test
    fun `transaction approval sign and submit error`() = runTest {
        coEvery { transactionClient.signTransaction(any(), any(), any(), any()) } returns Result.failure(
            DappRequestException(
                DappRequestFailure.TransactionApprovalFailure.SubmitNotarizedTransaction
            )
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
        coEvery { transactionClient.analyzeExecution(any(), any()) } returns Result.success(
            ExecutionAnalysis(
                feeLocks = FeeLocks(
                    lock = Decimal.zero(),
                    contingentLock = Decimal.zero()
                ),
                feeSummary = com.radixdlt.ret.FeeSummary(
                    executionCost = Decimal.zero(),
                    finalizationCost = Decimal.zero(),
                    storageExpansionCost = Decimal.zero(),
                    royaltyCost = Decimal.zero()
                ),
                transactionTypes = listOf(
                    TransactionType.GeneralTransaction(
                        accountProofs = listOf(),
                        accountWithdraws = mapOf(),
                        accountDeposits = mapOf(),
                        addressesInManifest = mapOf(),
                        metadataOfNewlyCreatedEntities = mapOf(),
                        dataOfNewlyMintedNonFungibles = mapOf(),
                        addressesOfNewlyCreatedEntities = listOf()
                    )
                ),
                reservedInstructions = emptyList()
            )
        )
        val vm = vm.value
        advanceUntilIdle()
        assertEquals("0.1083795", vm.state.value.transactionFees.networkFeeDisplayed)
        assertEquals("0", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.1083795", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 1`() = runTest {
        coEvery { transactionClient.analyzeExecution(any(), any()) } returns Result.success(
            ExecutionAnalysis(
                feeLocks = FeeLocks(
                    lock = Decimal("0.9"),
                    contingentLock = Decimal.zero()
                ),
                feeSummary = com.radixdlt.ret.FeeSummary(
                    executionCost = Decimal("0.3"),
                    finalizationCost = Decimal("0.3"),
                    storageExpansionCost = Decimal("0.2"),
                    royaltyCost = Decimal("0.2")
                ),
                transactionTypes = listOf(
                    TransactionType.GeneralTransaction(
                        accountProofs = listOf(),
                        accountWithdraws = mapOf(),
                        accountDeposits = mapOf(),
                        addressesInManifest = mapOf(),
                        metadataOfNewlyCreatedEntities = mapOf(),
                        dataOfNewlyMintedNonFungibles = mapOf(),
                        addressesOfNewlyCreatedEntities = listOf()
                    )
                ),
                reservedInstructions = emptyList()
            )
        )
        val vm = vm.value
        advanceUntilIdle()
        assertEquals("0.1283795", vm.state.value.transactionFees.networkFeeDisplayed)
        assertEquals("0.2", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.3283795", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 2`() = runTest {
        coEvery { transactionClient.analyzeExecution(any(), any()) } returns Result.success(
            ExecutionAnalysis(
                feeLocks = FeeLocks(
                    lock = Decimal("0.5"),
                    contingentLock = Decimal.zero()
                ),
                feeSummary = com.radixdlt.ret.FeeSummary(
                    executionCost = Decimal("0.3"),
                    finalizationCost = Decimal("0.3"),
                    storageExpansionCost = Decimal("0.2"),
                    royaltyCost = Decimal("0.2")
                ),
                transactionTypes = listOf(
                    TransactionType.GeneralTransaction(
                        accountProofs = listOf(),
                        accountWithdraws = mapOf(),
                        accountDeposits = mapOf(),
                        addressesInManifest = mapOf(),
                        metadataOfNewlyCreatedEntities = mapOf(),
                        dataOfNewlyMintedNonFungibles = mapOf(),
                        addressesOfNewlyCreatedEntities = listOf()
                    )
                ),
                reservedInstructions = emptyList()
            )
        )
        val vm = vm.value
        advanceUntilIdle()
        assertEquals("0.5283795", vm.state.value.transactionFees.networkFeeDisplayed)
        assertEquals("0.2", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.7283795", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 3`() = runTest {
        coEvery { transactionClient.analyzeExecution(any(), any()) } returns Result.success(
            ExecutionAnalysis(
                feeLocks = FeeLocks(
                    lock = Decimal("1.0"),
                    contingentLock = Decimal.zero()
                ),
                feeSummary = com.radixdlt.ret.FeeSummary(
                    executionCost = Decimal("0.3"),
                    finalizationCost = Decimal("0.3"),
                    storageExpansionCost = Decimal("0.2"),
                    royaltyCost = Decimal("0.2")
                ),
                transactionTypes = listOf(
                    TransactionType.GeneralTransaction(
                        accountProofs = listOf(),
                        accountWithdraws = mapOf(),
                        accountDeposits = mapOf(),
                        addressesInManifest = mapOf(),
                        metadataOfNewlyCreatedEntities = mapOf(),
                        dataOfNewlyMintedNonFungibles = mapOf(),
                        addressesOfNewlyCreatedEntities = listOf()
                    )
                ),
                reservedInstructions = emptyList()
            )
        )
        val vm = vm.value
        advanceUntilIdle()
        assertEquals("0.0283795", vm.state.value.transactionFees.networkFeeDisplayed)
        assertEquals("0.2", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.2283795", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 4`() = runTest {
        coEvery { transactionClient.analyzeExecution(any(), any()) } returns Result.success(
            ExecutionAnalysis(
                feeLocks = FeeLocks(
                    lock = Decimal("1.5"),
                    contingentLock = Decimal.zero()
                ),
                feeSummary = com.radixdlt.ret.FeeSummary(
                    executionCost = Decimal("0.3"),
                    finalizationCost = Decimal("0.3"),
                    storageExpansionCost = Decimal("0.2"),
                    royaltyCost = Decimal("0.2")
                ),
                transactionTypes = listOf(
                    TransactionType.GeneralTransaction(
                        accountProofs = listOf(),
                        accountWithdraws = mapOf(),
                        accountDeposits = mapOf(),
                        addressesInManifest = mapOf(),
                        metadataOfNewlyCreatedEntities = mapOf(),
                        dataOfNewlyMintedNonFungibles = mapOf(),
                        addressesOfNewlyCreatedEntities = listOf()
                    )
                ),
                reservedInstructions = emptyList()
            )
        )
        val vm = vm.value
        advanceUntilIdle()
        assertNull(vm.state.value.transactionFees.networkFeeDisplayed)
        assertEquals("0", vm.state.value.transactionFees.defaultRoyaltyFeesDisplayed)
        assertEquals("0", vm.state.value.transactionFees.defaultTransactionFee.displayableQuantity())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 4 with one signer account`()
        = runTest {
        coEvery { transactionClient.analyzeExecution(any(), any()) } returns Result.success(
            ExecutionAnalysis(
                feeLocks = FeeLocks(
                    lock = Decimal.zero(),
                    contingentLock = Decimal.zero()
                ),
                feeSummary = com.radixdlt.ret.FeeSummary(
                    executionCost = Decimal("0.3"),
                    finalizationCost = Decimal("0.3"),
                    storageExpansionCost = Decimal("0.2"),
                    royaltyCost = Decimal("0.2")
                ),
                transactionTypes = listOf(
                    TransactionType.GeneralTransaction(
                        accountProofs = listOf(),
                        accountWithdraws = mapOf(),
                        accountDeposits = mapOf(),
                        addressesInManifest = mapOf(),
                        metadataOfNewlyCreatedEntities = mapOf(),
                        dataOfNewlyMintedNonFungibles = mapOf(),
                        addressesOfNewlyCreatedEntities = listOf()
                    )
                ),
                reservedInstructions = emptyList()
            )
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
        coEvery { transactionClient.analyzeExecution(any(), any()) } returns Result.success(
            ExecutionAnalysis(
                feeLocks = FeeLocks(
                    lock = Decimal("1.5"),
                    contingentLock = Decimal.zero()
                ),
                feeSummary = com.radixdlt.ret.FeeSummary(
                    executionCost = Decimal("0.3"),
                    finalizationCost = Decimal("0.3"),
                    storageExpansionCost = Decimal("0.2"),
                    royaltyCost = Decimal("0.2")
                ),
                transactionTypes = listOf(
                    TransactionType.GeneralTransaction(
                        accountProofs = listOf(),
                        accountWithdraws = mapOf(),
                        accountDeposits = mapOf(),
                        addressesInManifest = mapOf(),
                        metadataOfNewlyCreatedEntities = mapOf(),
                        dataOfNewlyMintedNonFungibles = mapOf(),
                        addressesOfNewlyCreatedEntities = listOf()
                    )
                ),
                reservedInstructions = emptyList()
            )
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

        coEvery { transactionClient.analyzeExecution(any(), any()) } returns Result.success(
            ExecutionAnalysis(
                feeLocks = FeeLocks(
                    lock = Decimal("0.5"),
                    contingentLock = Decimal.zero()
                ),
                feeSummary = com.radixdlt.ret.FeeSummary(
                    executionCost = Decimal("0.3"),
                    finalizationCost = Decimal("0.3"),
                    storageExpansionCost = Decimal("0.2"),
                    royaltyCost = Decimal("0.2")
                ),
                transactionTypes = listOf(
                    TransactionType.GeneralTransaction(
                        accountProofs = listOf(),
                        accountWithdraws = mapOf(),
                        accountDeposits = mapOf(),
                        addressesInManifest = mapOf(),
                        metadataOfNewlyCreatedEntities = mapOf(),
                        dataOfNewlyMintedNonFungibles = mapOf(),
                        addressesOfNewlyCreatedEntities = listOf()
                    )
                ),
                reservedInstructions = emptyList()
            )
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

        coEvery { transactionClient.analyzeExecution(any(), any()) } returns Result.success(
            ExecutionAnalysis(
                feeLocks = FeeLocks(
                    lock = Decimal("0.5"),
                    contingentLock = Decimal.zero()
                ),
                feeSummary = com.radixdlt.ret.FeeSummary(
                    executionCost = Decimal("0.3"),
                    finalizationCost = Decimal("0.3"),
                    storageExpansionCost = Decimal("0.2"),
                    royaltyCost = Decimal("0.2")
                ),
                transactionTypes = listOf(
                    TransactionType.GeneralTransaction(
                        accountProofs = listOf(),
                        accountWithdraws = mapOf(),
                        accountDeposits = mapOf(),
                        addressesInManifest = mapOf(),
                        metadataOfNewlyCreatedEntities = mapOf(),
                        dataOfNewlyMintedNonFungibles = mapOf(),
                        addressesOfNewlyCreatedEntities = listOf()
                    )
                ),
                reservedInstructions = emptyList()
            )
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.onFeePaddingAmountChanged(feePaddingAmount)
        vm.onTipPercentageChanged(tipPercentage)

        assertEquals(expectedFeeLock, vm.state.value.transactionFees.transactionFeeToLock.displayableQuantity())
    }

    private fun previewResponse() = TransactionPreviewResponse(
        encodedReceipt = "",
        receipt = TransactionReceipt(
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
