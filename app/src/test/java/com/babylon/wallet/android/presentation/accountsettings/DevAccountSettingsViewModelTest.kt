package com.babylon.wallet.android.presentation.accountsettings

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.domain.usecases.FaucetState
import com.babylon.wallet.android.domain.usecases.GetFreeXrdUseCase
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.account.settings.ARG_ACCOUNT_SETTINGS_ADDRESS
import com.babylon.wallet.android.presentation.account.settings.devsettings.DevAccountSettingsViewModel
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.AddAuthSigningFactorInstanceUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class DevAccountSettingsViewModelTest : StateViewModelTest<DevAccountSettingsViewModel>() {

    private val getFreeXrdUseCase = mockk<GetFreeXrdUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val addAuthSigningFactorInstanceUseCase = mockk<AddAuthSigningFactorInstanceUseCase>()
    private val transactionStatusClient = mockk<TransactionStatusClient>()
    private val rolaClient = mockk<ROLAClient>()
    private val eventBus = mockk<AppEventBus>()
    private val sampleTxId = "txId1"
    private val sampleProfile = sampleDataProvider.sampleProfile()
    private val sampleAddress = sampleProfile.currentNetwork.accounts.first().address

    override fun initVM(): DevAccountSettingsViewModel {
        return DevAccountSettingsViewModel(
            getFreeXrdUseCase,
            getProfileUseCase,
            rolaClient,
            incomingRequestRepository,
            addAuthSigningFactorInstanceUseCase,
            transactionStatusClient,
            TestScope(),
            savedStateHandle,
            eventBus
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { getFreeXrdUseCase.getFaucetState(any()) } returns flowOf(FaucetState.Available(true))
        every { getProfileUseCase() } returns flowOf(sampleDataProvider.sampleProfile())
        coEvery { getFreeXrdUseCase(any()) } returns Result.success(sampleTxId)
        every { savedStateHandle.get<String>(ARG_ACCOUNT_SETTINGS_ADDRESS) } returns sampleAddress
        coEvery { eventBus.sendEvent(any()) } just Runs
        every { rolaClient.signingState } returns emptyFlow()
    }

    @Test
    fun `initial state is correct when free xrd enabled`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        val state = vm.state.first()
        assert(state.faucetState is FaucetState.Available)
    }

    @Test
    fun `initial state is correct when free xrd not enabled`() = runTest {
        every { getFreeXrdUseCase.getFaucetState(any()) } returns flow { emit(FaucetState.Available(false)) }
        val vm = vm.value
        advanceUntilIdle()
        val state = vm.state.first()
        assert(state.faucetState is FaucetState.Available && !(state.faucetState as FaucetState.Available).isEnabled)
    }

    @Test
    fun `get free xrd success sets proper state`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onGetFreeXrdClick()
        advanceUntilIdle()
        coVerify(exactly = 1) { eventBus.sendEvent(AppEvent.RefreshResourcesNeeded) }
        coVerify(exactly = 1) { getFreeXrdUseCase(sampleAddress) }
    }

    @Test
    fun `get free xrd failure sets proper state`() = runTest {
        coEvery { getFreeXrdUseCase(any()) } returns Result.failure(Exception())
        val vm = vm.value
        advanceUntilIdle()
        vm.onGetFreeXrdClick()
        advanceUntilIdle()
        val state = vm.state.first()
        coVerify(exactly = 1) { getFreeXrdUseCase(sampleAddress) }
        assert(state.error != null)
    }
}
