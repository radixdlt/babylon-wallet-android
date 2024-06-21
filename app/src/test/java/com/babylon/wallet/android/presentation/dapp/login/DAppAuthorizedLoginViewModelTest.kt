@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.dapp.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.BuildAuthorizedDappResponseUseCase
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.fakes.DAppConnectionRepositoryFake
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.ARG_INTERACTION_ID
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.utils.AppEventBusImpl
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.extensions.AuthorizedDapps
import com.radixdlt.sargon.extensions.Personas
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.ProfileNetworks
import com.radixdlt.sargon.extensions.ReferencesToAuthorizedPersonas
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import rdx.works.core.domain.DApp
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.metadata.PublicKeyHash
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
                            referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas().asList()
                        )
                    ).asList()
                )
            ).asList()
        )
    }
    private val samplePersona = sampleProfile.networks.asIdentifiable().getBy(NetworkId.MAINNET)!!.personas.first()

    private val requestWithNonExistingDappAddress = IncomingMessage.IncomingRequest.AuthorizedRequest(
        remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata(
            NetworkId.MAINNET,
            "",
            AccountAddress.sampleMainnet().string,
            false
        ),
        authRequest = IncomingMessage.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge,
        oneTimeAccountsRequestItem = null,
        ongoingAccountsRequestItem = IncomingMessage.IncomingRequest.AccountsRequestItem(
            true,
            IncomingMessage.IncomingRequest.NumberOfValues(
                1,
                IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.AtLeast
            ),
            null
        )
    )

    private val usePersonaRequestOngoing = IncomingMessage.IncomingRequest.AuthorizedRequest(
        remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata(
            NetworkId.MAINNET,
            "",
            AccountAddress.sampleMainnet().string,
            false
        ),
        authRequest = IncomingMessage.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(IdentityAddress.sampleMainnet()),
        ongoingAccountsRequestItem = IncomingMessage.IncomingRequest.AccountsRequestItem(
            true, IncomingMessage.IncomingRequest.NumberOfValues(
                1,
                IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.AtLeast
            ),
            null
        )
    )

    private val usePersonaRequestOngoingPlusOngoingData = IncomingMessage.IncomingRequest.AuthorizedRequest(
        remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata(
            NetworkId.MAINNET,
            "",
            AccountAddress.sampleMainnet().string,
            false
        ),
        authRequest = IncomingMessage.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(IdentityAddress.sampleMainnet()),
        ongoingAccountsRequestItem = IncomingMessage.IncomingRequest.AccountsRequestItem(
            true, IncomingMessage.IncomingRequest.NumberOfValues(
                1,
                IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.AtLeast
            ),
            null
        ),
        ongoingPersonaDataRequestItem = IncomingMessage.IncomingRequest.PersonaRequestItem(
            isRequestingName = true,
            isOngoing = true
        )
    )

    private val usePersonaRequestOneTimeAccounts = IncomingMessage.IncomingRequest.AuthorizedRequest(
        remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata(
            NetworkId.MAINNET,
            "",
            AccountAddress.sampleMainnet().string,
            false
        ),
        authRequest = IncomingMessage.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(IdentityAddress.sampleMainnet()),
        oneTimeAccountsRequestItem = IncomingMessage.IncomingRequest.AccountsRequestItem(
            false, IncomingMessage.IncomingRequest.NumberOfValues(
                1,
                IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.AtLeast
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
            getCurrentGatewayUseCase,
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
        every { buildAuthorizedDappResponseUseCase.signingState } returns emptyFlow()
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
        vm.onSelectPersona(samplePersona)
        advanceUntilIdle()
        vm.personaSelectionConfirmed()
        advanceUntilIdle()
        vm.oneOffEvent.test {
            assert(expectMostRecentItem() is Event.DisplayPermission)
        }
        vm.onAccountsSelected(listOf(AccountItemUiModel(AccountAddress.sampleMainnet(), "account 1", AppearanceId(0u))), false)
        advanceUntilIdle()
        vm.oneOffEvent.test {
            val mostRecentItem = expectMostRecentItem()
            assert(mostRecentItem is Event.RequestCompletionBiometricPrompt)
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
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.Permission)
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
            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.ChooseAccount)
        }
    }

    private class StateRepositoryFake : StateRepository {
        override fun observeAccountsOnLedger(accounts: List<Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> {
            TODO("Not yet implemented")
        }

        override suspend fun getNextNFTsPage(
            account: Account,
            resource: Resource.NonFungibleResource
        ): Result<Resource.NonFungibleResource> {
            TODO("Not yet implemented")
        }

        override suspend fun updateLSUsInfo(
            account: Account,
            validatorsWithStakes: List<ValidatorWithStakes>
        ): Result<List<ValidatorWithStakes>> {
            TODO("Not yet implemented")
        }

        override suspend fun updateStakeClaims(account: Account, claims: List<StakeClaim>): Result<List<StakeClaim>> {
            TODO("Not yet implemented")
        }

        override suspend fun getResources(
            addresses: Set<ResourceAddress>,
            underAccountAddress: AccountAddress?,
            withDetails: Boolean
        ): Result<List<Resource>> {
            TODO("Not yet implemented")
        }

        override suspend fun getPools(poolAddresses: Set<PoolAddress>): Result<List<Pool>> {
            TODO("Not yet implemented")
        }

        override suspend fun getValidators(validatorAddresses: Set<ValidatorAddress>): Result<List<Validator>> {
            TODO("Not yet implemented")
        }

        override suspend fun getNFTDetails(
            resourceAddress: ResourceAddress,
            localIds: Set<NonFungibleLocalId>
        ): Result<List<Resource.NonFungibleResource.Item>> {
            TODO("Not yet implemented")
        }

        override suspend fun getOwnedXRD(accounts: List<Account>): Result<Map<Account, Decimal192>> {
            TODO("Not yet implemented")
        }

        override suspend fun getEntityOwnerKeys(entities: List<ProfileEntity>): Result<Map<ProfileEntity, List<PublicKeyHash>>> {
            TODO("Not yet implemented")
        }

        override suspend fun getDAppsDetails(definitionAddresses: List<AccountAddress>, isRefreshing: Boolean): Result<List<DApp>> {
            return Result.success(
                definitionAddresses.mapIndexed { index, accountAddress ->
                    DApp(
                        dAppAddress = accountAddress,
                        metadata = listOf(
                            Metadata.Primitive(ExplicitMetadataKey.NAME.key, "dApp $index", MetadataType.String)
                        )
                    )
                }
            )
        }

        override suspend fun getDAppDefinitions(componentAddresses: List<ComponentAddress>): Result<Map<ComponentAddress, AccountAddress?>> {
            TODO("Not yet implemented")
        }

        override suspend fun cacheNewlyCreatedResources(newResources: List<Resource>): Result<Unit> {
            TODO("Not yet implemented")
        }

        override suspend fun clearCachedState(): Result<Unit> {
            TODO("Not yet implemented")
        }

    }

}
