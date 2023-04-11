@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.dapp.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.PersonaDataField
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.fakes.DappMessengerFake
import com.babylon.wallet.android.fakes.DappMetadataRepositoryFake
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginEvent
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ARG_REQUEST_ID
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase

class DAppAuthorizedLoginViewModelTest : StateViewModelTest<DAppAuthorizedLoginViewModel>() {

    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val dappMetadataRepository = DappMetadataRepositoryFake()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val dAppMessenger = DappMessengerFake()
    private val dAppConnectionRepository = spyk<DAppConnectionRepositoryFake> { DAppConnectionRepositoryFake() }

    private val samplePersona = SampleDataProvider().samplePersona(personaAddress = "address1")

    private val requestWithNonExistingDappAddress = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        dappId = "dappId",
        requestId = "1",
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
        dappId = "dappId",
        requestId = "1",
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

    private val usePersonaRequestOngoingPlusOngoingData = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        dappId = "1",
        requestId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address"
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest("address1"),
        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            true, false, 1,
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
        ),
        ongoingPersonaDataRequestItem = MessageFromDataChannel.IncomingRequest.PersonaRequestItem(listOf(PersonaDataField.GivenName), true)
    )

    private val usePersonaRequestOngoingDataOnly = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        dappId = "1",
        requestId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address"
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest("address1"),
        ongoingPersonaDataRequestItem = MessageFromDataChannel.IncomingRequest.PersonaRequestItem(listOf(PersonaDataField.GivenName), true)
    )

    private val usePersonaRequestOneTimeAccounts = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        dappId = "1",
        requestId = "1",
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

    private val usePersonaRequestOneTimeAccountsAndData = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        dappId = "1",
        requestId = "1",
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            11,
            "",
            "address"
        ),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest("address1"),
        oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            false, false, 1,
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
        ),
        oneTimePersonaDataRequestItem = MessageFromDataChannel.IncomingRequest.PersonaRequestItem(listOf(PersonaDataField.GivenName), false)
    )

    override fun initVM(): DAppAuthorizedLoginViewModel {
        return DAppAuthorizedLoginViewModel(
            savedStateHandle,
            dAppMessenger,
            dAppConnectionRepository,
            getProfileUseCase,
            getCurrentGatewayUseCase,
            dappMetadataRepository,
            incomingRequestRepository
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_REQUEST_ID) } returns "1"
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.nebunet
        every { getProfileUseCase() } returns flowOf(profile(
            personas = listOf(samplePersona),
            dApps = listOf(Network.AuthorizedDapp(
                networkID = Radix.Gateway.nebunet.network.id,
                dAppDefinitionAddress = "dapp_address",
                displayName = "1",
                referencesToAuthorizedPersonas = emptyList()
            ))
        ))
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns requestWithNonExistingDappAddress
    }

    @Test
    fun `init sets correct state for login request`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.SelectPersona)
        }
    }

    @Test
    fun `handle ongoing request flow first time`() = runTest {
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.SavedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.onSelectPersona(samplePersona)
        advanceUntilIdle()
        vm.personaSelectionConfirmed()
        advanceUntilIdle()
        vm.oneOffEvent.test {
            assert(expectMostRecentItem() is DAppAuthorizedLoginEvent.DisplayPermission)
        }
        vm.onAccountsSelected(listOf(AccountItemUiModel("random address", "account 1", 0)), false)
        advanceUntilIdle()
        vm.oneOffEvent.test {
            assert(expectMostRecentItem() is DAppAuthorizedLoginEvent.LoginFlowCompleted)
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
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.Permission)
        }
    }

    @Test
    fun `init sets correct state for use persona accounts and data when accounts are already granted`() = runTest {
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns usePersonaRequestOngoingPlusOngoingData
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        coEvery { dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(any(), any(), any(), any()) } returns listOf(
            SampleDataProvider().randomAddress()
        )
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingPersonaData)
        }
    }

    @Test
    fun `init sets correct state for use persona onetime request`() = runTest {
        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns usePersonaRequestOneTimeAccounts
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.ChooseAccount)
        }
    }

}
