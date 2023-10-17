package com.babylon.wallet.android.presentation.accountsettings

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.usecases.FaucetState
import com.babylon.wallet.android.domain.usecases.GetFreeXrdUseCase
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.account.settings.ARG_ACCOUNT_SETTINGS_ADDRESS
import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel
import com.babylon.wallet.android.presentation.account.settings.Event
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.domain.ChangeEntityVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.RenameAccountDisplayNameUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountSettingsViewModelTest : StateViewModelTest<AccountSettingsViewModel>() {

    private val getFreeXrdUseCase = mockk<GetFreeXrdUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val renameAccountDisplayNameUseCase = mockk<RenameAccountDisplayNameUseCase>()
    private val changeEntityVisibilityUseCase = mockk<ChangeEntityVisibilityUseCase>()
    private val sampleProfile = sampleDataProvider.sampleProfile()
    private val sampleAddress = sampleProfile.currentNetwork.accounts.first().address

    override fun initVM(): AccountSettingsViewModel {
        return AccountSettingsViewModel(
            getFreeXrdUseCase,
            getProfileUseCase,
            renameAccountDisplayNameUseCase,
            savedStateHandle,
            changeEntityVisibilityUseCase
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { getFreeXrdUseCase.getFaucetState(any()) } returns flowOf(FaucetState.Available(true))
        every { getProfileUseCase() } returns flowOf(sampleDataProvider.sampleProfile())
        every { savedStateHandle.get<String>(ARG_ACCOUNT_SETTINGS_ADDRESS) } returns sampleAddress
        coEvery { changeEntityVisibilityUseCase.hideAccount(any()) } just Runs
    }

    @Test
    fun `hide account sets proper state and fire one-off event`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onHideAccount()
        advanceUntilIdle()
        coVerify(exactly = 1) { changeEntityVisibilityUseCase.hideAccount(any()) }
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
            assert(item.isNewNameValid)
        }
        vm.onRenameAccountNameChange("new name very very very very very very very very very very very long")
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.isNewNameValid.not())
            assert(item.isNewNameLengthMoreThanTheMaximum)
        }
    }

}
