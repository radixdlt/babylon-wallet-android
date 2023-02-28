package com.babylon.wallet.android.presentation.settings.connecteddapps

import app.cash.turbine.test
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.presentation.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AuthorizedDappsViewModelTest : BaseViewModelTest<AuthorizedDappsViewModel>() {

    private val dAppConnectionRepository = DAppConnectionRepositoryFake()

    override fun initVM(): AuthorizedDappsViewModel {
        return AuthorizedDappsViewModel(dAppConnectionRepository)
    }

    @Test
    fun `init load dapp data into state`() = runTest {
        val vm = vm.value
        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.dapps.size == 2)
        }
        collectJob.cancel()
    }
}