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
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.usecases.GetDAppWithMetadataAndAssociatedResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionComponentResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionProofResourcesUseCase
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import com.radixdlt.toolkit.models.method.AnalyzeTransactionExecutionOutput
import com.radixdlt.toolkit.models.method.EncounteredAddresses
import com.radixdlt.toolkit.models.method.EncounteredComponents
import com.radixdlt.toolkit.models.method.NewlyCreated
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import com.babylon.wallet.android.domain.common.Result as ResultInternal

@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionApprovalViewModelTest : StateViewModelTest<TransactionApprovalViewModel>() {

    private val transactionClient = mockk<TransactionClient>()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getTransactionComponentResourcesUseCase = mockk<GetTransactionComponentResourcesUseCase>()
    private val getTransactionProofResourcesUseCase = mockk<GetTransactionProofResourcesUseCase>()
    private val getDAppWithAssociatedResourcesUseCase = mockk<GetDAppWithMetadataAndAssociatedResourcesUseCase>()
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
    private val sampleManifest = sampleDataProvider.sampleManifest()

    @Before
    override fun setUp() = runTest {
        super.setUp()
        every { deviceSecurityHelper.isDeviceSecure() } returns true
        every { savedStateHandle.get<String>(ARG_TRANSACTION_REQUEST_ID) } returns sampleRequestId
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.nebunet
        coEvery { getTransactionProofResourcesUseCase.invoke(any()) } returns listOf(
            PresentingProofUiModel("", "")
        )
        coEvery { transactionClient.signAndSubmitTransaction(any()) } returns Result.success(sampleTxId)
        coEvery { transactionClient.manifestInStringFormat(any()) } returns Result.success(sampleManifest)
        coEvery { transactionClient.findFeePayerInManifest(any()) } returns Result.success(FeePayerSearchResult("feePayer"))
        coEvery { transactionClient.signingState } returns emptyFlow()
        coEvery { transactionClient.convertManifestInstructionsToJSON(any()) } returns Result.success(sampleManifest)
        coEvery { transactionClient.getTransactionPreview(any(), any(), any()) } returns Result.success(
            previewResponse()
        )
        coEvery { transactionClient.analyzeManifestWithPreviewContext(any(), any()) } returns Result.success(
            analyzeManifestResponse()
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
    }

    override fun initVM(): TransactionApprovalViewModel {
        return TransactionApprovalViewModel(
            transactionClient,
            getTransactionComponentResourcesUseCase,
            getTransactionProofResourcesUseCase,
            incomingRequestRepository,
            getCurrentGatewayUseCase,
            deviceSecurityHelper,
            dAppMessenger,
            getDAppWithAssociatedResourcesUseCase,
            TestScope(),
            appEventBus,
            savedStateHandle
        )
    }

    @Test
    fun `transaction approval success`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.approveTransaction()
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
        vm.approveTransaction()
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
        assert(vm.oneOffEvent.first() is TransactionApprovalEvent.Dismiss)
    }

    @Test
    fun `transaction approval sign and submit error`() = runTest {
        coEvery { transactionClient.signAndSubmitTransaction(any()) } returns Result.failure(
            DappRequestException(
                DappRequestFailure.TransactionApprovalFailure.SubmitNotarizedTransaction
            )
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.approveTransaction()
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

    private fun analyzeManifestResponse() = AnalyzeTransactionExecutionOutput(
        encounteredAddresses = EncounteredAddresses(
            EncounteredComponents(
                emptyArray(),
                emptyArray(),
                emptyArray(),
                emptyArray(),
                emptyArray(),
                emptyArray()
            ),
            emptyArray(),
            emptyArray()
        ),
        accountsRequiringAuth = emptyArray(),
        accountProofResources = emptyArray(),
        accountWithdraws = emptyArray(),
        accountDeposits = emptyArray(),
        newlyCreated = NewlyCreated(
            resources = emptyArray()
        )
    )
}
