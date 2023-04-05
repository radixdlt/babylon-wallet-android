package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.wallet.WalletUiState
import com.babylon.wallet.android.presentation.wallet.WalletViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.domain.GetProfileStateUseCase
import rdx.works.profile.domain.GetProfileUseCase

@ExperimentalCoroutinesApi
class WalletViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private lateinit var vm: WalletViewModel
    private val requestAccountsUseCase = mock(GetAccountResourcesUseCase::class.java)
    private val getProfileUseCase = mock<GetProfileUseCase>()
    private val getProfileStateUseCase = mock<GetProfileStateUseCase>()

    private val sampleData = SampleDataProvider().sampleAccountResource()

    @Before
    fun setUp() {
        vm = WalletViewModel(
            requestAccountsUseCase,
            getProfileStateUseCase,
            getProfileUseCase
        )
        whenever(getProfileStateUseCase()).thenReturn(flowOf(ProfileState.Restored(profile())))
        whenever(getProfileUseCase()).thenReturn(flowOf(profile()))
    }

    @Test
    fun `when view model init, verify initial value of account UI state is Loading`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()

        // when
        vm.state
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        assertTrue(event.first().isLoading)
    }

    @Test
    fun `when view model init, verify account Ui state content is loaded at the end`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()
        whenever(requestAccountsUseCase.getAccountsFromProfile(isRefreshing = false))
            .thenReturn(Result.Success(listOf(sampleData)))

        // when
        vm.state
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // then
        advanceUntilIdle()
        assertTrue(!event.last().isLoading)
    }
}
