@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.dapp.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.domain.usecases.login.BuildAuthorizedDappResponseUseCase
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.fakes.StateRepositoryFake
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.ARG_INTERACTION_ID
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.utils.AppEventBusImpl
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedDappPreferenceDeposits
import com.radixdlt.sargon.AuthorizedDappPreferences
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.AuthorizedDapps
import com.radixdlt.sargon.extensions.Personas
import com.radixdlt.sargon.extensions.ProfileNetworks
import com.radixdlt.sargon.extensions.ReferencesToAuthorizedPersonas
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import java.util.UUID

class DAppAuthorizedLoginViewModelTest : StateViewModelTest<DAppAuthorizedLoginViewModel>() {

    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val appEventBus = mockk<AppEventBusImpl>()
    private val stateRepository = StateRepositoryFake()
    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val buildAuthorizedDappResponseUseCase = mockk<BuildAuthorizedDappResponseUseCase>()
    private val respondToIncomingRequestUseCase = mockk<RespondToIncomingRequestUseCase>()
    private val dAppConnectionRepository = spyk<DAppConnectionRepositoryFake> { DAppConnectionRepositoryFake() }

    private val sampleProfile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities().let { profile ->
        val network = profile.networks.asIdentifiable().getBy(NetworkId.MAINNET)!!
        val persona = network.personas.first()
        profile.copy(
            networks = ProfileNetworks(
                network.copy(
                    personas = Personas(persona).asList(),
                    authorizedDapps = AuthorizedDapps(
                        AuthorizedDapp(
                            networkId = NetworkId.MAINNET,
                            dappDefinitionAddress = AccountAddress.sampleMainnet(),
                            displayName = "1",
                            referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas().asList(),
                            preferences = AuthorizedDappPreferences(
                                deposits = AuthorizedDappPreferenceDeposits.VISIBLE
                            )
                        )
                    ).asList()
                )
            ).asList()
        )
    }
    private val samplePersona = sampleProfile.networks.asIdentifiable().getBy(NetworkId.MAINNET)!!.personas.first()

    private val requestWithNonExistingDappAddress = WalletAuthorizedRequest(
        remoteEntityId = RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = DappToWalletInteraction.RequestMetadata(
            NetworkId.MAINNET,
            "",
            AccountAddress.sampleMainnet().string,
            false
        ),
        authRequestItem = WalletAuthorizedRequest.AuthRequestItem.LoginRequest.WithoutChallenge,
        oneTimeAccountsRequestItem = null,
        ongoingAccountsRequestItem = DappToWalletInteraction.AccountsRequestItem(
            true,
            DappToWalletInteraction.NumberOfValues(
                1,
                DappToWalletInteraction.NumberOfValues.Quantifier.AtLeast
            ),
            null
        )
    )

    private val usePersonaRequestOngoing = WalletAuthorizedRequest(
        remoteEntityId = RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = DappToWalletInteraction.RequestMetadata(
            NetworkId.MAINNET,
            "",
            AccountAddress.sampleMainnet().string,
            false
        ),
        authRequestItem = WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest(IdentityAddress.sampleMainnet()),
        ongoingAccountsRequestItem = DappToWalletInteraction.AccountsRequestItem(
            true, DappToWalletInteraction.NumberOfValues(
                1,
                DappToWalletInteraction.NumberOfValues.Quantifier.AtLeast
            ),
            null
        )
    )

    private val usePersonaRequestOngoingPlusOngoingData = WalletAuthorizedRequest(
        remoteEntityId = RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = DappToWalletInteraction.RequestMetadata(
            NetworkId.MAINNET,
            "",
            AccountAddress.sampleMainnet().string,
            false
        ),
        authRequestItem = WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest(IdentityAddress.sampleMainnet()),
        ongoingAccountsRequestItem = DappToWalletInteraction.AccountsRequestItem(
            true, DappToWalletInteraction.NumberOfValues(
                1,
                DappToWalletInteraction.NumberOfValues.Quantifier.AtLeast
            ),
            null
        ),
        ongoingPersonaDataRequestItem = DappToWalletInteraction.PersonaDataRequestItem(
            isRequestingName = true,
            isOngoing = true
        )
    )

    private val usePersonaRequestOneTimeAccounts = WalletAuthorizedRequest(
        remoteEntityId = RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = DappToWalletInteraction.RequestMetadata(
            NetworkId.MAINNET,
            "",
            AccountAddress.sampleMainnet().string,
            false
        ),
        authRequestItem = WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest(IdentityAddress.sampleMainnet()),
        oneTimeAccountsRequestItem = DappToWalletInteraction.AccountsRequestItem(
            false, DappToWalletInteraction.NumberOfValues(
                1,
                DappToWalletInteraction.NumberOfValues.Quantifier.AtLeast
            ),
            null
        )
    )

    override fun initVM(): DAppAuthorizedLoginViewModel {
        return DAppAuthorizedLoginViewModel(
            savedStateHandle,
            appEventBus,
            respondToIncomingRequestUseCase,
            dAppConnectionRepository,
            getProfileUseCase,
            stateRepository,
            incomingRequestRepository,
            buildAuthorizedDappResponseUseCase
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { appEventBus.events } returns emptyFlow()
        every { savedStateHandle.get<String>(ARG_INTERACTION_ID) } returns "1"
        coEvery { getCurrentGatewayUseCase() } returns Gateway.forNetwork(NetworkId.MAINNET)
        coEvery { buildAuthorizedDappResponseUseCase.invoke(any(), any(), any(), any(), any(), any()) } returns Result.success(any())
        coEvery { getProfileUseCase() } returns sampleProfile
        coEvery { incomingRequestRepository.getRequest(any()) } returns requestWithNonExistingDappAddress
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
        advanceUntilIdle()
        vm.onPersonaAuthorized(samplePersona.asProfileEntity(), null)
        advanceUntilIdle()
        vm.oneOffEvent.test {
            assert(expectMostRecentItem() is Event.DisplayPermission)
        }
    }

    @Test
    fun `init sets correct state for use persona ongoing request`() = runTest {
        coEvery { incomingRequestRepository.getRequest(any()) } returns usePersonaRequestOngoing
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingAccounts)
        }
    }

    @Test
    fun `init sets correct state for use persona accounts and data when accounts are already granted`() = runTest {
        coEvery { incomingRequestRepository.getRequest(any()) } returns usePersonaRequestOngoingPlusOngoingData
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        coEvery { dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(any(), any(), any(), any()) } returns listOf(
            AccountAddress.sampleMainnet.random()
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
        coEvery { incomingRequestRepository.getRequest(any()) } returns usePersonaRequestOneTimeAccounts
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OneTimeAccounts)
        }
    }

}
