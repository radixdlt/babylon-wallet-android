package com.babylon.wallet.android.presentation.settings.accountsecurity

import app.cash.turbine.test
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.settings.accountsecurity.depositguarantees.DepositGuaranteesViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Transaction
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.depositguarantees.ChangeDefaultDepositGuaranteeUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class DepositGuaranteesViewModelTest : StateViewModelTest<DepositGuaranteesViewModel>() {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val changeDefaultDepositGuaranteeUseCase = mockk<ChangeDefaultDepositGuaranteeUseCase>()

    override fun initVM(): DepositGuaranteesViewModel {
        return DepositGuaranteesViewModel(
            getProfileUseCase = getProfileUseCase,
            changeDefaultDepositGuaranteeUseCase = changeDefaultDepositGuaranteeUseCase
        )
    }

    @Before
    override fun setUp() {
        super.setUp()

        coEvery { getProfileUseCase() } returns flowOf(
            profile(transaction = Transaction(defaultDepositGuarantee = 1.5))
        )
    }

    @Test
    fun `when view init, verify default deposit guarantee is shown`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.isDepositInputValid)
            assert(item.depositGuarantee == "150")
        }
    }

    @Test
    fun `when deposit guaranteed increase button pressed, verify number shown is increased`() = runTest {
        // given
        coEvery { changeDefaultDepositGuaranteeUseCase.invoke(any()) } returns Unit
        val vm = vm.value

        // when
        vm.onDepositGuaranteeIncreased()
        advanceUntilIdle()

        // then
        val state = vm.state.first()
        assert(state.isDepositInputValid)
        assert(state.depositGuarantee == "151")
        coVerify(exactly = 1) { changeDefaultDepositGuaranteeUseCase.invoke(1.51) }
    }

    @Test
    fun `when deposit guaranteed decrease button pressed, verify number shown is decreased`() = runTest {
        // given
        coEvery { changeDefaultDepositGuaranteeUseCase.invoke(any()) } returns Unit
        val vm = vm.value

        // when
        vm.onDepositGuaranteeDecreased()
        advanceUntilIdle()

        // then
        val state = vm.state.first()
        assert(state.isDepositInputValid)
        assert(state.depositGuarantee == "149")
        coVerify(exactly = 1) { changeDefaultDepositGuaranteeUseCase.invoke(1.49) }
    }

    @Test
    fun `when valid deposit guarantees field input provided, verify it is shown properly`() = runTest {
        // given
        coEvery { changeDefaultDepositGuaranteeUseCase.invoke(any()) } returns Unit
        coEvery { getProfileUseCase() } returns flowOf(
            profile(transaction = Transaction(defaultDepositGuarantee = 2.0))
        )
        val vm = vm.value

        // when
        vm.onDepositGuaranteeChanged("200")
        advanceUntilIdle()

        // then
        val state = vm.state.first()
        assert(state.isDepositInputValid)
        assert(state.depositGuarantee == "200")
        coVerify(exactly = 1) { changeDefaultDepositGuaranteeUseCase.invoke(2.0) }
    }

    @Test
    fun `when invalid deposit guarantees field input provided, verify error shown and input not saved`() = runTest {
        // given
        coEvery { changeDefaultDepositGuaranteeUseCase.invoke(any()) } returns Unit
        coEvery { getProfileUseCase() } returns flowOf(
            profile(transaction = Transaction(defaultDepositGuarantee = 2.0))
        )
        val vm = vm.value
        advanceUntilIdle()

        // when
        vm.onDepositGuaranteeChanged(".")
        advanceUntilIdle()

        // then
        val state = vm.state.first()
        assert(!state.isDepositInputValid)
        assert(state.depositGuarantee == ".")
        coVerify(exactly = 0) { changeDefaultDepositGuaranteeUseCase.invoke(any()) }
    }

    @Test
    fun `when longer input is entered, do not round number and save it as is`() = runTest {
        // given
        coEvery { changeDefaultDepositGuaranteeUseCase.invoke(any()) } returns Unit
        coEvery { getProfileUseCase() } returns flowOf(
            profile(transaction = Transaction(defaultDepositGuarantee = 2.0))
        )
        val vm = vm.value
        advanceUntilIdle()

        // when
        vm.onDepositGuaranteeChanged("99.9999")
        advanceUntilIdle()

        // then
        val state = vm.state.first()
        assert(state.isDepositInputValid)
        assert(state.depositGuarantee == "99.9999")
        coVerify(exactly = 1) { changeDefaultDepositGuaranteeUseCase.invoke(any()) }
    }
}