package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.account.AccountUiState
import com.babylon.wallet.android.presentation.account.AccountViewModel
import com.babylon.wallet.android.presentation.account.history.ARG_ACCOUNT_ADDRESS
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.getBy
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private lateinit var vm: AccountViewModel

    private val getWalletAssetsUseCase = mockk<GetWalletAssetsUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val appEventBus = Mockito.mock(AppEventBus::class.java)
    private val savedStateHandle = Mockito.mock(SavedStateHandle::class.java)
    private val profile = Profile.sample()
    private val account = profile.networks.getBy(NetworkId.MAINNET)?.accounts?.invoke()?.first()!!

    @Before
    fun setUp() = runTest {
        coEvery { preferencesManager.getBackedUpFactorSourceIds() } returns flowOf(emptySet())
        every { getProfileUseCase.flow } returns flowOf(profile)
        whenever(savedStateHandle.get<String>(ARG_ACCOUNT_ADDRESS)).thenReturn(account.address.string)
        whenever(appEventBus.events).thenReturn(MutableSharedFlow<AppEvent>().asSharedFlow())
    }

//    @Test
//    fun `when viewmodel init, verify loading displayed before loading account ui`() = runTest {
//        // given
//        val event = mutableListOf<AccountUiState>()
//        vm = AccountViewModel(getWalletAssetsUseCase, getProfileUseCase, preferencesManager, appEventBus, savedStateHandle)
//        vm.state
//            .onEach { event.add(it) }
//            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
//
//        advanceUntilIdle()
//
//        // then
//        Assert.assertEquals(event.first().isLoading, true)
//    }
//
//    @Test
//    fun `when viewmodel init, verify accountUi loaded after loading`() = runTest {
//        // given
//        val event = mutableListOf<AccountUiState>()
//        vm = AccountViewModel(requestAccountsUseCase, getProfileUseCase, preferencesManager, appEventBus, savedStateHandle)
//        vm.state
//            .onEach { event.add(it) }
//            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
//
//        advanceUntilIdle()
//
//        // then
//        with(event.last()) {
//            assert(!this.isLoading)
//            assert(xrdToken != null)
//            assert(sampleData.fungibleResources.size == 3)
//        }
//    }
}
