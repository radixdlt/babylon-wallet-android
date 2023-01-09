package com.babylon.wallet.android.presentation.accountpreference

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.usecases.GetFreeXrdUseCase
import com.babylon.wallet.android.presentation.BaseViewModelTest
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountPreferenceViewModelTest : BaseViewModelTest<AccountPreferenceViewModel>() {

    private val getFreeXrdUseCase = mockk<GetFreeXrdUseCase>()
    private val deviceSecurityHelper = mockk<DeviceSecurityHelper>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val eventBus = mockk<AppEventBus>()
    private val sampleTxId = "txId1"
    private val sampleAddress = sampleDataProvider.randomTokenAddress()

    override fun initVM(): AccountPreferenceViewModel {
        return AccountPreferenceViewModel(getFreeXrdUseCase, deviceSecurityHelper, TestScope(), savedStateHandle, eventBus)
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { deviceSecurityHelper.isDeviceSecure() } returns true
        every { getFreeXrdUseCase.isAllowedToUseFaucet(any()) } returns flow { emit(true) }
        coEvery { getFreeXrdUseCase(true, any()) } returns Result.Success(sampleTxId)
        every { savedStateHandle.get<String>(AddressArg) } returns sampleAddress
        coEvery { eventBus.sendEvent(any()) } just Runs
    }

    @Test
    fun `initial state is correct when free xrd enabled`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        assert(vm.state.isDeviceSecure)
        assert(vm.state.canUseFaucet)
    }

    @Test
    fun `initial state is correct when free xrd not enabled`() = runTest {
        every { getFreeXrdUseCase.isAllowedToUseFaucet(any()) } returns flow { emit(false) }
        val vm = vm.value
        advanceUntilIdle()
        assert(vm.state.isDeviceSecure)
        assert(!vm.state.canUseFaucet)
    }

    @Test
    fun `get free xrd success sets proper state`() = runTest {
        val vm = vm.value
        vm.onGetFreeXrdClick()
        advanceUntilIdle()
        coVerify(exactly = 1) { getFreeXrdUseCase(true, sampleAddress) }
        assert(vm.state.gotFreeXrd)
    }

    @Test
    fun `get free xrd failure sets proper state`() = runTest {
        coEvery { getFreeXrdUseCase(true, any()) } returns Result.Error(Exception())
        val vm = vm.value
        vm.onGetFreeXrdClick()
        advanceUntilIdle()
        coVerify(exactly = 1) { getFreeXrdUseCase(true, sampleAddress) }
        assert(!vm.state.gotFreeXrd)
        assert(vm.state.error != null)
    }

}