package com.babylon.wallet.android.presentation.accountsettings

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.usecases.FaucetState
import com.babylon.wallet.android.domain.usecases.GetFreeXrdUseCase
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourceIntegrityStatusMessagesUseCase
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.account.settings.ARG_ACCOUNT_SETTINGS_ADDRESS
import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel
import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel.Event
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
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
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.core.sargon.updateLastUsed
import rdx.works.profile.domain.ChangeEntityVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.RenameAccountDisplayNameUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountSettingsViewModelTest : StateViewModelTest<AccountSettingsViewModel>() {

    private val getFreeXrdUseCase = mockk<GetFreeXrdUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val renameAccountDisplayNameUseCase = mockk<RenameAccountDisplayNameUseCase>()
    private val getFactorSourceIntegrityStatusMessagesUseCase = mockk<GetFactorSourceIntegrityStatusMessagesUseCase>()
    private val changeEntityVisibilityUseCase = mockk<ChangeEntityVisibilityUseCase>()
    private val sampleProfile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities()
    private val sampleAddress = sampleProfile.currentNetwork!!.accounts.first().address
    private val eventBus = mockk<AppEventBus>()
    private val sampleTxId = "txId1"

    override fun initVM(): AccountSettingsViewModel {
        return AccountSettingsViewModel(
            getFreeXrdUseCase,
            getProfileUseCase,
            renameAccountDisplayNameUseCase,
            savedStateHandle,
            changeEntityVisibilityUseCase,
            getFactorSourceIntegrityStatusMessagesUseCase,
            TestScope(),
            eventBus
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { getFreeXrdUseCase.getFaucetState(any()) } returns flowOf(FaucetState.Available(true))
        coEvery { getFreeXrdUseCase(any()) } returns Result.success(sampleTxId)
        coEvery { getProfileUseCase() } returns sampleProfile
        every { getProfileUseCase.flow } returns flowOf(sampleProfile)
        every { savedStateHandle.get<String>(ARG_ACCOUNT_SETTINGS_ADDRESS) } returns sampleAddress.string
        coEvery { changeEntityVisibilityUseCase.changeAccountVisibility(any(), any()) } just Runs
        coEvery { eventBus.sendEvent(any()) } just Runs
        coEvery { getFactorSourceIntegrityStatusMessagesUseCase.forFactorSource(any(), any(), any()) } returns emptyList()
    }

    @Test
    fun `hide account sets proper state and fire one-off event`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onHideAccount()
        advanceUntilIdle()
        coVerify(exactly = 1) { changeEntityVisibilityUseCase.changeAccountVisibility(any(), any()) }
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is Event.AccountHidden)
        }
    }

    @Test
    fun `account rename valid & invalid`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onRenameAccountNameChange("new valid name")
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.renameAccountInput.isNameValid)
        }
        vm.onRenameAccountNameChange("new name very very very very very very very very very very very long")
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.renameAccountInput.isNameValid.not())
            assert(item.renameAccountInput.isNameTooLong)
        }
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
        coVerify(exactly = 1) { eventBus.sendEvent(AppEvent.RefreshAssetsNeeded) }
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
