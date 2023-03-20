@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.dapp.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.fakes.AccountRepositoryFake
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.fakes.DappMessengerFake
import com.babylon.wallet.android.fakes.DappMetadataRepositoryFake
import com.babylon.wallet.android.presentation.BaseViewModelTest
import com.babylon.wallet.android.presentation.dapp.InitialDappLoginRoute
import com.babylon.wallet.android.presentation.dapp.account.AccountItemUiModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.data.repository.ProfileDataSource

class DAppLoginViewModelTest : BaseViewModelTest<DAppLoginViewModel>() {

    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val dappMetadataRepository = DappMetadataRepositoryFake()
    private val profileDataSource = mockk<ProfileDataSource>()
    private val accountRepository = AccountRepositoryFake()
    private val personaRepository = mockk<PersonaRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val dAppMessenger = DappMessengerFake()
    private val dAppConnectionRepository = DAppConnectionRepositoryFake()

    private val samplePersona = SampleDataProvider().samplePersona()

    private val requestWithNonExistingDappAddress = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address"
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest(),
        oneTimeAccountsRequestItem = null,
        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            true, false, 1,
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
        )
    )

    private val usePersonaRequestOngoing = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address"
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest("address1"),
        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            true, false, 1,
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
        )
    )

    private val usePersonaRequestOneTime = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address"
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest("address1"),
        oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            false, false, 1,
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
        )
    )

    override fun initVM(): DAppLoginViewModel {
        return DAppLoginViewModel(
            savedStateHandle,
            dAppMessenger,
            dAppConnectionRepository,
            personaRepository,
            accountRepository,
            profileDataSource,
            dappMetadataRepository,
            incomingRequestRepository
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        val addressSlot = slot<String>()
        every { savedStateHandle.get<String>(ARG_REQUEST_ID) } returns "1"
        coEvery { profileDataSource.getCurrentNetwork() } returns Radix.Network.nebunet
        coEvery { personaRepository.getPersonaByAddress(capture(addressSlot)) } answers {
            SampleDataProvider().samplePersona(addressSlot.captured)
        }
        coEvery { personaRepository.personas } returns flow {
            emit(listOf(samplePersona))
        }
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns requestWithNonExistingDappAddress
    }

    @Test
    fun `init sets correct state for login request`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialDappLoginRoute is InitialDappLoginRoute.SelectPersona)
        }
    }

    @Test
    fun `handle ongoing request flow first time`() = runTest {
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.SavedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.onSelectPersona(samplePersona)
        advanceUntilIdle()
        vm.onLogin()
        advanceUntilIdle()
        vm.oneOffEvent.test {
            assert(expectMostRecentItem() is DAppLoginEvent.DisplayPermission)
        }
        vm.onAccountsSelected(listOf(AccountItemUiModel("random address", "account 1", 0)), false)
        advanceUntilIdle()
        vm.oneOffEvent.test {
            assert(expectMostRecentItem() is DAppLoginEvent.LoginFlowCompleted)
        }
    }

    @Test
    fun `init sets correct state for use persona ongoing request`() = runTest {
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns usePersonaRequestOngoing
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialDappLoginRoute is InitialDappLoginRoute.Permission)
        }
    }

    @Test
    fun `init sets correct state for use persona onetime request`() = runTest {
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns usePersonaRequestOneTime
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialDappLoginRoute is InitialDappLoginRoute.ChooseAccount)
        }
    }

}
