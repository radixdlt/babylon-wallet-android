package com.babylon.wallet.android.presentation.settings.authorizeddapps

import app.cash.turbine.test
import rdx.works.core.domain.DApp
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.radixdlt.sargon.AccountAddress
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AuthorizedDappsViewModelTest : StateViewModelTest<AuthorizedDappsViewModel>() {

    private val dAppConnectionRepository = DAppConnectionRepositoryFake()
    private val getDAppsUseCase = mockk<GetDAppsUseCase>()

    override fun initVM(): AuthorizedDappsViewModel {
        return AuthorizedDappsViewModel(getDAppsUseCase, dAppConnectionRepository)
    }

    @Before
    override fun setUp() {
        super.setUp()

        coEvery { getDAppsUseCase(any<Set<AccountAddress>>(), false) } returns Result.success(DApp.sample.all)
    }

    @Test
    fun `init load dapp data into state`() = runTest {
        val vm = vm.value
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.dApps.size == 2)
        }
        collectJob.cancel()
    }
}