package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.ARG_REQUEST_SOURCE
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountConfirmationEvent
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountConfirmationViewModel
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.navigation.Screen
import com.radixdlt.sargon.Accounts
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileNetworks
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.getBy
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.core.sargon.changeGateway
import rdx.works.profile.domain.GetProfileUseCase

@ExperimentalCoroutinesApi
class CreateAccountConfirmationViewModelTest : StateViewModelTest<CreateAccountConfirmationViewModel>() {

    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).let {
        val mainnetNetwork = it.networks.getBy(NetworkId.MAINNET)!!.let { network ->
            network.copy(accounts = Accounts.init(network.accounts().take(1)))
        }
        it.copy(networks = ProfileNetworks.init(mainnetNetwork))
    }
    private val account = profile.networks.getBy(NetworkId.MAINNET)?.accounts?.invoke()?.first()!!

    @Before
    override fun setUp() = runTest {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_ACCOUNT_ID) } returns account.address.string
        every { savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME) } returns account.displayName.value
        every { savedStateHandle.get<Boolean>(Screen.ARG_HAS_PROFILE) } returns false
        every { getProfileUseCase.flow } returns flowOf(profile)
        coEvery { getProfileUseCase() } returns profile
    }

    @Test
    fun `given profile did not exist, when view model init, verify correct account state and go next`() = runTest {
        // given
        every { savedStateHandle.get<CreateAccountRequestSource>(ARG_REQUEST_SOURCE) } returns CreateAccountRequestSource.FirstTime
        val viewModel = vm.value
        val event = mutableListOf<CreateAccountConfirmationEvent>()

        // when
        viewModel.accountConfirmed()

        viewModel.oneOffEvent
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreateAccountConfirmationViewModel.AccountConfirmationUiState(
                accountName = account.displayName.value,
                accountAddress = account.address.string,
                appearanceId = AppearanceId(0u)
            ),
            viewModel.state.value
        )

        Assert.assertEquals(event.first(), CreateAccountConfirmationEvent.NavigateToHome)
    }

    @Test
    fun `given profile did exist, when view model init, verify correct account state and dismiss`() = runTest {
        every { savedStateHandle.get<CreateAccountRequestSource>(ARG_REQUEST_SOURCE) } returns CreateAccountRequestSource.AccountsList
        val event = mutableListOf<CreateAccountConfirmationEvent>()
        val viewModel = vm.value
        viewModel.accountConfirmed()

        viewModel.oneOffEvent
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreateAccountConfirmationViewModel.AccountConfirmationUiState(
                accountName = account.displayName.value,
                accountAddress = account.address.string,
                appearanceId = AppearanceId(0u)
            ),
            viewModel.state.value
        )

        Assert.assertEquals(event.first(), CreateAccountConfirmationEvent.FinishAccountCreation)
    }

    override fun initVM(): CreateAccountConfirmationViewModel {
        return CreateAccountConfirmationViewModel(getProfileUseCase, savedStateHandle)
    }
}
