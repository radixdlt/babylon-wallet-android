package com.babylon.wallet.android.presentation.accountpreference

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.usecases.GetFreeXrdUseCase
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.utils.AppEventBus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.AddAuthSigningFactorInstanceUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountPreferenceViewModelTest : StateViewModelTest<AccountPreferenceViewModel>() {

    private val getFreeXrdUseCase = mockk<GetFreeXrdUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val addAuthSigningFactorInstanceUseCase = mockk<AddAuthSigningFactorInstanceUseCase>()
    private val rolaClient = mockk<ROLAClient>()
    private val eventBus = mockk<AppEventBus>()
    private val sampleTxId = "txId1"
    private val sampleAddress = sampleDataProvider.randomAddress()

    override fun initVM(): AccountPreferenceViewModel {
        return AccountPreferenceViewModel(
            getFreeXrdUseCase,
            getProfileUseCase,
            rolaClient,
            incomingRequestRepository,
            addAuthSigningFactorInstanceUseCase,
            TestScope(),
            savedStateHandle,
            eventBus
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { getFreeXrdUseCase.isAllowedToUseFaucet(any()) } returns flow { emit(true) }
        every { getProfileUseCase() } returns flowOf(SampleDataProvider().sampleProfile())
        coEvery { getFreeXrdUseCase(any()) } returns Result.success(sampleTxId)
        every { savedStateHandle.get<String>(ARG_ADDRESS) } returns sampleAddress
        coEvery { eventBus.sendEvent(any()) } just Runs
    }

    @Test
    fun `initial state is correct when free xrd enabled`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        val state = vm.state.first()
        assert(state.canUseFaucet)
    }

    @Test
    fun `initial state is correct when free xrd not enabled`() = runTest {
        every { getFreeXrdUseCase.isAllowedToUseFaucet(any()) } returns flow { emit(false) }
        val vm = vm.value
        advanceUntilIdle()
        val state = vm.state.first()
        assert(!state.canUseFaucet)
    }

    @Test
    fun `get free xrd success sets proper state`() = runTest {
        val vm = vm.value
        vm.onGetFreeXrdClick()
        advanceUntilIdle()
        val state = vm.state.first()
        coVerify(exactly = 1) { getFreeXrdUseCase(sampleAddress) }
        assert(state.gotFreeXrd)
    }

    @Test
    fun `get free xrd failure sets proper state`() = runTest {
        coEvery { getFreeXrdUseCase(any()) } returns Result.failure(Exception())
        val vm = vm.value
        vm.onGetFreeXrdClick()
        advanceUntilIdle()
        val state = vm.state.first()
        coVerify(exactly = 1) { getFreeXrdUseCase(sampleAddress) }
        assert(!state.gotFreeXrd)
        assert(state.error != null)
    }
}
