package com.babylon.wallet.android.presentation

import android.content.ClipData
import android.content.ClipboardManager
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
import com.babylon.wallet.android.fakes.AccountRepositoryFake
import com.babylon.wallet.android.presentation.wallet.WalletUiState
import com.babylon.wallet.android.presentation.wallet.WalletViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import rdx.works.profile.data.repository.ProfileDataSource

@ExperimentalCoroutinesApi
class WalletViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private lateinit var vm: WalletViewModel
    private val requestAccountsUseCase = mock(GetAccountResourcesUseCase::class.java)
    private val clipboardManager = mock(ClipboardManager::class.java)
    private val profileDataSource = mock(ProfileDataSource::class.java)
    private val accountRepository = AccountRepositoryFake()

    private val profile = SampleDataProvider().sampleProfile()

    private val sampleData = SampleDataProvider().sampleAccountResource()

    @Before
    fun setUp() {
        vm = WalletViewModel(clipboardManager, requestAccountsUseCase, profileDataSource, accountRepository)
        whenever(profileDataSource.profile).thenReturn(flow { emit(profile) })
    }

    @Test
    fun `when view model init, verify initial value of wallet UI state is Loading`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()

        // when
        vm.walletUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        assertTrue(event.first().isLoading)
    }

    @Test
    fun `when view model init, verify initial value of account UI state is Loading`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()

        // when
        vm.walletUiState
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

        // when
        val viewModel = WalletViewModel(clipboardManager, requestAccountsUseCase, profileDataSource, accountRepository)
        whenever(requestAccountsUseCase(isRefreshing = false)).thenReturn(Result.Success(listOf(sampleData)))
        viewModel.walletUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        assertTrue(!event.last().isLoading)
    }

    @Test
    fun `when onCopy called, verify content copied to clipboard manager`() {
        // given
        val hash = "somehash2123"
        val clipData = ClipData.newPlainText("accountHash", hash)
        vm.onCopyAccountAddress(hash)

        // then
        verify(clipboardManager).setPrimaryClip(clipData)
    }
}
