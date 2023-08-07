package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.gateway.generated.models.FeeSummary
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionReceipt
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Badge
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesMetadataUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppsUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.ExecutionAnalysis
import com.radixdlt.ret.FeeLocks
import com.radixdlt.ret.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import java.math.BigDecimal
import com.babylon.wallet.android.domain.common.Result as ResultInternal

@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionApprovalViewModelTest : StateViewModelTest<TransactionApprovalViewModel>() {

    private val transactionClient = mockk<TransactionClient>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getAccountsWithResourcesUseCase = mockk<GetAccountsWithResourcesUseCase>()
    private val getResourcesMetadataUseCase = mockk<GetResourcesMetadataUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getTransactionBadgesUseCase = mockk<GetTransactionBadgesUseCase>()
    private val resolveDAppsUseCase = mockk<ResolveDAppsUseCase>()
    private val incomingRequestRepository = IncomingRequestRepositoryImpl()
    private val dAppMessenger = mockk<DappMessenger>()
    private val appEventBus = mockk<AppEventBus>()
    private val deviceSecurityHelper = mockk<DeviceSecurityHelper>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val sampleTxId = "txId1"
    private val sampleRequestId = "requestId1"
    private val sampleRequest = MessageFromDataChannel.IncomingRequest.TransactionRequest(
        dappId = "dappId",
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
        resourceAddress = "addr_xrd",
        amount = BigDecimal.TEN,
        symbolMetadataItem = SymbolMetadataItem("XRD")
    )

    @Before
    override fun setUp() = runTest {
        super.setUp()
        every { deviceSecurityHelper.isDeviceSecure() } returns true
        every { savedStateHandle.get<String>(ARG_TRANSACTION_REQUEST_ID) } returns sampleRequestId
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.nebunet
        coEvery { getTransactionBadgesUseCase.invoke(any()) } returns listOf(
            Badge(address = "", nameMetadataItem = null, iconMetadataItem = null)
        )
        coEvery { transactionClient.signAndSubmitTransaction(any(), any(), any(), any()) } returns Result.success(sampleTxId)
        coEvery { transactionClient.findFeePayerInManifest(any(), any()) } returns Result.success(FeePayerSearchResult("feePayer"))
        coEvery { transactionClient.signingState } returns emptyFlow()
        coEvery { transactionClient.getTransactionPreview(any(), any()) } returns Result.success(
            previewResponse()
        )
        coEvery {
            dAppMessenger.sendTransactionWriteResponseSuccess(
                dappId = "dappId",
                requestId = sampleRequestId,
                txId = sampleTxId
            )
        } returns ResultInternal.Success(Unit)
        coEvery {
            dAppMessenger.sendWalletInteractionResponseFailure(
                dappId = "dappId",
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
                    networkFee = Decimal.zero(),
                    royaltyFee = Decimal.zero()
                ),
                transactionType = TransactionType.GeneralTransaction(
                    accountProofs = listOf(),
                    accountWithdraws = mapOf(),
                    accountDeposits = mapOf(),
                    addressesInManifest = mapOf(),
                    metadataOfNewlyCreatedEntities = mapOf(),
                    dataOfNewlyMintedNonFungibles = mapOf(),
                    addressesOfNewlyCreatedEntities = listOf()
                )
            )
        )
        every { getProfileUseCase() } returns flowOf(profile(accounts = listOf(fromAccount) + otherAccounts))
        coEvery {
            getAccountsWithResourcesUseCase(
                accounts = any(),
                isRefreshing = false
            )
        } returns com.babylon.wallet.android.domain.common.Result.Success(
            listOf(
                AccountWithResources(
                    account = sampleProfile.currentNetwork.accounts[0],
                    resources = Resources(fungibleResources = listOf(sampleXrdResource), nonFungibleResources = emptyList())
                ),
                AccountWithResources(
                    account = sampleProfile.currentNetwork.accounts[0],
                    resources = Resources(fungibleResources = emptyList(), nonFungibleResources = emptyList())
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

    override fun initVM(): TransactionApprovalViewModel {
        return TransactionApprovalViewModel(
            transactionClient = transactionClient,
            getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase,
            getResourcesMetadataUseCase = getResourcesMetadataUseCase,
            getProfileUseCase = getProfileUseCase,
            getTransactionBadgesUseCase = getTransactionBadgesUseCase,
            resolveDAppsUseCase = resolveDAppsUseCase,
            getCurrentGatewayUseCase = getCurrentGatewayUseCase,
            dAppMessenger = dAppMessenger,
            appEventBus = appEventBus,
            incomingRequestRepository = incomingRequestRepository,
            deviceSecurityHelper = deviceSecurityHelper,
            appScope = TestScope(),
            savedStateHandle = savedStateHandle
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
                dappId = "dappId",
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
                dappId = "dappId",
                requestId = sampleRequestId,
                error = capture(errorSlot),
                message = any()
            )
        }
        assert(errorSlot.captured == WalletErrorType.WrongNetwork)
        assert(vm.oneOffEvent.first() is TransactionApprovalViewModel.Event.Dismiss)
    }

    @Test
    fun `transaction approval sign and submit error`() = runTest {
        coEvery { transactionClient.signAndSubmitTransaction(any(), any(), any(), any()) } returns Result.failure(
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
                dappId = "dappId",
                requestId = sampleRequestId,
                error = capture(errorSlot),
                message = any()
            )
        }
        assert(errorSlot.captured == WalletErrorType.FailedToSubmitTransaction)
        assert(state.error != null)
    }

    // TODO test TransactionFees

    private fun previewResponse() = TransactionPreviewResponse(
        encodedReceipt = "",
        receipt = TransactionReceipt(
            status = "",
            feeSummary = FeeSummary(
                cost_unit_limit = 1,
                cost_unit_price = "",
                cost_units_consumed = 1,
                tip_percentage = 1,
                xrd_royalty_receivers = emptyList(),
                xrd_total_execution_cost = "",
                xrd_total_royalty_cost = "",
                xrd_total_tipped = "",
            ),
            errorMessage = ""
        ),
        logs = emptyList()
    )
}
