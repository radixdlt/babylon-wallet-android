@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.dapp.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Ignore

@Ignore("TODO Integration")
class DAppAuthorizedLoginViewModelTest/* : StateViewModelTest<DAppAuthorizedLoginViewModel>()*/ {
//
//    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
//    private val appEventBus = mockk<AppEventBus>()
//    private val stateRepository = StateRepositoryFake()
//    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
//    private val getProfileUseCase = mockk<GetProfileUseCase>()
//    private val savedStateHandle = mockk<SavedStateHandle>()
//    private val buildAuthorizedDappResponseUseCase = mockk<BuildAuthorizedDappResponseUseCase>()
//    private val dAppMessenger = DappMessengerFake()
//    private val dAppConnectionRepository = spyk<DAppConnectionRepositoryFake> { DAppConnectionRepositoryFake() }
//
//    private val samplePersona = SampleDataProvider().samplePersona(personaAddress = IdentityAddress.sampleMainnet().string)
//
//    private val requestWithNonExistingDappAddress = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
//        remoteConnectorId = "remoteConnectorId",
//        interactionId = "1",
//        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
//            NetworkId.MAINNET.discriminant.toInt(),
//            "",
//            AccountAddress.sampleMainnet().string,
//            false
//        ),
//        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge,
//        oneTimeAccountsRequestItem = null,
//        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
//            true,
//            MessageFromDataChannel.IncomingRequest.NumberOfValues(
//                1,
//                MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
//            ),
//            null
//        )
//    )
//
//    private val usePersonaRequestOngoing = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
//        remoteConnectorId = "remoteConnectorId",
//        interactionId = "1",
//        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
//            NetworkId.MAINNET.discriminant.toInt(),
//            "",
//            AccountAddress.sampleMainnet().string,
//            false
//        ),
//        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(IdentityAddress.sampleMainnet().string),
//        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
//            true, MessageFromDataChannel.IncomingRequest.NumberOfValues(
//                1,
//                MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
//            ),
//            null
//        )
//    )
//
//    private val usePersonaRequestOngoingPlusOngoingData = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
//        remoteConnectorId = "1",
//        interactionId = "1",
//        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
//            NetworkId.MAINNET.discriminant.toInt(),
//            "",
//            AccountAddress.sampleMainnet().string,
//            false
//        ),
//        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(IdentityAddress.sampleMainnet().string),
//        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
//            true, MessageFromDataChannel.IncomingRequest.NumberOfValues(
//                1,
//                MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
//            ),
//            null
//        ),
//        ongoingPersonaDataRequestItem = MessageFromDataChannel.IncomingRequest.PersonaRequestItem(
//            isRequestingName = true,
//            isOngoing = true
//        )
//    )
//
//    private val usePersonaRequestOneTimeAccounts = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
//        remoteConnectorId = "1",
//        interactionId = "1",
//        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
//            NetworkId.MAINNET.discriminant.toInt(),
//            "",
//            AccountAddress.sampleMainnet().string,
//            false
//        ),
//        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(IdentityAddress.sampleMainnet().string),
//        oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
//            false, MessageFromDataChannel.IncomingRequest.NumberOfValues(
//                1,
//                MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
//            ),
//            null
//        )
//    )
//
//    private val usePersonaRequestOneTimeAccountsAndData = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
//        remoteConnectorId = "1",
//        interactionId = "1",
//        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
//            NetworkId.MAINNET.discriminant.toInt(),
//            "",
//            AccountAddress.sampleMainnet().string,
//            false
//        ),
//        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(IdentityAddress.sampleMainnet().string),
//        oneTimeAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
//            false, MessageFromDataChannel.IncomingRequest.NumberOfValues(
//                1,
//                MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
//            ),
//            null
//        ),
//        oneTimePersonaDataRequestItem = MessageFromDataChannel.IncomingRequest.PersonaRequestItem(
//            isRequestingName = true,
//            isOngoing = false
//        )
//    )
//
//    override fun initVM(): DAppAuthorizedLoginViewModel {
//        return DAppAuthorizedLoginViewModel(
//            savedStateHandle,
//            appEventBus,
//            dAppMessenger,
//            dAppConnectionRepository,
//            getProfileUseCase,
//            getCurrentGatewayUseCase,
//            stateRepository,
//            incomingRequestRepository,
//            buildAuthorizedDappResponseUseCase
//        )
//    }
//
//    @Before
//    override fun setUp() {
//        super.setUp()
//        every { savedStateHandle.get<String>(ARG_INTERACTION_ID) } returns "1"
//        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.mainnet
//        every { buildAuthorizedDappResponseUseCase.signingState } returns emptyFlow()
//        coEvery { buildAuthorizedDappResponseUseCase.invoke(any(), any(), any(), any(), any(), any(),) } returns Result.success(any())
//        every { getProfileUseCase() } returns flowOf(
//            profile(
//                personas = identifiedArrayListOf(samplePersona),
//                dApps = listOf(
//                    Network.AuthorizedDapp(
//                        networkID = Radix.Gateway.mainnet.network.id,
//                        dAppDefinitionAddress = AccountAddress.sampleMainnet().string,
//                        displayName = "1",
//                        referencesToAuthorizedPersonas = emptyList()
//                    )
//                )
//            )
//        )
//        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns requestWithNonExistingDappAddress
//    }
//
//    @Test
//    fun `init sets correct state for login request`() = runTest {
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.SelectPersona)
//        }
//    }
//
//    @Test
//    fun `handle ongoing request flow first time`() = runTest {
//        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.SavedDapp
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.onSelectPersona(samplePersona)
//        advanceUntilIdle()
//        vm.personaSelectionConfirmed()
//        advanceUntilIdle()
//        vm.oneOffEvent.test {
//            assert(expectMostRecentItem() is Event.DisplayPermission)
//        }
//        vm.onAccountsSelected(listOf(AccountItemUiModel(AccountAddress.sampleMainnet(), "account 1", 0)), false)
//        advanceUntilIdle()
//        vm.oneOffEvent.test {
//            val mostRecentItem = expectMostRecentItem()
//            assert(mostRecentItem is Event.RequestCompletionBiometricPrompt)
//        }
//    }
//
//    @Test
//    fun `init sets correct state for use persona ongoing request`() = runTest {
//        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns usePersonaRequestOngoing
//        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.Permission)
//        }
//    }
//
//    @Test
//    fun `init sets correct state for use persona accounts and data when accounts are already granted`() = runTest {
//        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns usePersonaRequestOngoingPlusOngoingData
//        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
//        coEvery { dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(any(), any(), any(), any()) } returns listOf(
//            AccountAddress.sampleMainnet.random()
//        )
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingPersonaData)
//        }
//    }
//
//    @Test
//    fun `init sets correct state for use persona onetime request`() = runTest {
//        coEvery { incomingRequestRepository.getAuthorizedRequest(any()) } returns usePersonaRequestOneTimeAccounts
//        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.ChooseAccount)
//        }
//    }
//
//    private class StateRepositoryFake: StateRepository {
//        override fun observeAccountsOnLedger(accounts: List<Network.Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> {
//            error("Not needed")
//        }
//
//        override suspend fun getNextNFTsPage(
//            account: Network.Account,
//            resource: Resource.NonFungibleResource
//        ): Result<Resource.NonFungibleResource> {
//            error("Not needed")
//        }
//
//        override suspend fun updateLSUsInfo(
//            account: Network.Account,
//            validatorsWithStakes: List<ValidatorWithStakes>
//        ): Result<List<ValidatorWithStakes>> {
//            error("Not needed")
//        }
//
//        override suspend fun updateStakeClaims(account: Network.Account, claims: List<StakeClaim>): Result<List<StakeClaim>> {
//            error("Not needed")
//        }
//
//        override suspend fun getResources(
//            addresses: Set<ResourceAddress>,
//            underAccountAddress: AccountAddress?,
//            withDetails: Boolean
//        ): Result<List<Resource>> {
//            return Result.success(emptyList())
//        }
//
//        override suspend fun getPools(poolAddresses: Set<PoolAddress>): Result<List<Pool>> {
//            error("Not needed")
//        }
//
//        override suspend fun getValidators(validatorAddresses: Set<ValidatorAddress>): Result<List<Validator>> {
//            error("Not needed")
//        }
//
//        override suspend fun getNFTDetails(
//            resourceAddress: ResourceAddress,
//            localIds: Set<NonFungibleLocalId>
//        ): Result<List<Resource.NonFungibleResource.Item>> {
//            error("Not needed")
//        }
//
//        override suspend fun getOwnedXRD(accounts: List<Network.Account>): Result<Map<Network.Account, Decimal192>> {
//            error("Not needed")
//        }
//
//        override suspend fun getEntityOwnerKeys(entities: List<Entity>): Result<Map<Entity, List<PublicKeyHash>>> {
//            error("Not needed")
//        }
//
//        override suspend fun getDAppsDetails(definitionAddresses: List<AccountAddress>, isRefreshing: Boolean): Result<List<DApp>> {
//            return Result.success(
//                definitionAddresses.mapIndexed { index, accountAddress ->
//                    DApp(
//                        dAppAddress = accountAddress,
//                        metadata = listOf(
//                            Metadata.Primitive(ExplicitMetadataKey.NAME.key, "dApp $index", MetadataType.String)
//                        )
//                    )
//                }
//            )
//        }
//
//        override suspend fun getDAppDefinitions(componentAddresses: List<ComponentAddress>): Result<Map<ComponentAddress, AccountAddress?>> {
//            error("Not needed")
//        }
//
//        override suspend fun cacheNewlyCreatedResources(newResources: List<Resource>): Result<Unit> {
//            error("Not needed")
//        }
//
//        override suspend fun clearCachedState(): Result<Unit> {
//            error("Not needed")
//        }
//
//    }
//
}
