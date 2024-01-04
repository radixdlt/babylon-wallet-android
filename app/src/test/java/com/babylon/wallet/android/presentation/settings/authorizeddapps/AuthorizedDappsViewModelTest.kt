package com.babylon.wallet.android.presentation.settings.authorizeddapps

import app.cash.turbine.test
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.usecases.GetDAppWithResourcesUseCase
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
    private val getDAppWithAssociatedResourcesUseCase = mockk<GetDAppWithResourcesUseCase>()

    override fun initVM(): AuthorizedDappsViewModel {
        return AuthorizedDappsViewModel(getDAppWithAssociatedResourcesUseCase, dAppConnectionRepository)
    }

    @Before
    override fun setUp() {
        super.setUp()

        coEvery { getDAppWithAssociatedResourcesUseCase("address1", false) } returns
                Result.success(
                    SampleDataProvider().sampleDAppWithResources()
                )
        coEvery { getDAppWithAssociatedResourcesUseCase("address2", false) } returns
                Result.success(
                    SampleDataProvider().sampleDAppWithResources()
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