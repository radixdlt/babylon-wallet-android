package com.babylon.wallet.android.presentation.settings.authorizeddapps

import app.cash.turbine.test
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.presentation.StateViewModelTest
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

        coEvery { getDAppsUseCase(setOf("address1", "address2"), false) } returns Result.success(
            listOf(DApp(dAppAddress = "address1"), DApp(dAppAddress = "address2"))
        )
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