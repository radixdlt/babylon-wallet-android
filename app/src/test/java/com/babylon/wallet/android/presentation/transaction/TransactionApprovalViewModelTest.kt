package com.babylon.wallet.android.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.data.transaction.TransactionApprovalException
import com.babylon.wallet.android.data.transaction.TransactionApprovalFailure
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.presentation.BaseViewModelTest
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.derivation.model.NetworkId

@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionApprovalViewModelTest : BaseViewModelTest<TransactionApprovalViewModel>() {

    private val transactionClient = mockk<TransactionClient>()
    private val profileRepository = mockk<ProfileRepository>()
    private val incomingRequestRepository = IncomingRequestRepository()
    private val dAppMessenger = mockk<DAppMessenger>()
    private val deviceSecurityHelper = mockk<DeviceSecurityHelper>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val sampleTxId = "txId1"
    private val sampleRequestId = "requestId1"
    private val sampleRequest = MessageFromDataChannel.IncomingRequest.TransactionWriteRequest(
        sampleRequestId,
        11,
        TransactionManifestData("", 1, 11)
    )

    @Before
    override fun setUp() = runTest {
        super.setUp()
        every { deviceSecurityHelper.isDeviceSecure() } returns true
        every { savedStateHandle.get<String>(ARG_REQUEST_ID) } returns sampleRequestId
        coEvery { profileRepository.getCurrentNetworkId() } returns NetworkId.Nebunet
        coEvery { transactionClient.signAndSubmitTransaction(any()) } returns Result.Success(sampleTxId)
        coEvery {
            dAppMessenger.sendTransactionWriteResponseSuccess(sampleRequestId,
                sampleTxId)
        } returns Result.Success(Unit)
        coEvery {
            dAppMessenger.sendTransactionWriteResponseFailure(sampleRequestId,
                any(),
                any())
        } returns Result.Success(Unit)
        incomingRequestRepository.add(sampleRequest)
    }

    override fun initVM(): TransactionApprovalViewModel {
        return TransactionApprovalViewModel(transactionClient,
            incomingRequestRepository,
            profileRepository,
            deviceSecurityHelper,
            dAppMessenger,
            TestScope(),
            savedStateHandle)
    }

    @Test
    fun `init sets state properly`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        assert(vm.state.manifestData == sampleRequest.transactionManifestData)
    }

    @Test
    fun `transaction approval success`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.approveTransaction()
        advanceUntilIdle()
        assert(vm.state.approved)
        coVerify(exactly = 1) {
            dAppMessenger.sendTransactionWriteResponseSuccess(sampleRequestId, sampleTxId)
        }
    }

    @Test
    fun `transaction approval wrong network`() = runTest {
        coEvery { profileRepository.getCurrentNetworkId() } returns NetworkId.Hammunet
        val vm = vm.value
        advanceUntilIdle()
        vm.approveTransaction()
        advanceUntilIdle()
        val errorSlot = slot<WalletErrorType>()
        coVerify(exactly = 1) {
            dAppMessenger.sendTransactionWriteResponseFailure(sampleRequestId,
                capture(errorSlot),
                any())
        }
        assert(errorSlot.captured == WalletErrorType.WrongNetwork)
        assert(vm.oneOffEvent.first() is TransactionApprovalEvent.NavigateBack)
    }

    @Test
    fun `transaction approval sign and submit error`() = runTest {
        coEvery { transactionClient.signAndSubmitTransaction(any()) } returns Result.Error(TransactionApprovalException(
            TransactionApprovalFailure.SubmitNotarizedTransaction))
        val vm = vm.value
        advanceUntilIdle()
        vm.approveTransaction()
        advanceUntilIdle()
        val errorSlot = slot<WalletErrorType>()
        coVerify(exactly = 1) {
            dAppMessenger.sendTransactionWriteResponseFailure(sampleRequestId,
                capture(errorSlot),
                any())
        }
        assert(errorSlot.captured == WalletErrorType.FailedToSubmitTransaction)
        assert(vm.state.error != null)
    }

}