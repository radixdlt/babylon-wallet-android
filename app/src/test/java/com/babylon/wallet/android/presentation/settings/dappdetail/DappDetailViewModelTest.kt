package com.babylon.wallet.android.presentation.settings.dappdetail

/*
@OptIn(ExperimentalCoroutinesApi::class)
internal class DappDetailViewModelTest : StateViewModelTest<DappDetailViewModel>() {

    private val incomingRequestRepository = IncomingRequestRepositoryImpl()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getDAppWithAssociatedResourcesUseCase = mockk<GetDAppWithResourcesUseCase>()
    private val getValidatedDAppWebsiteUseCase = mockk<GetValidatedDAppWebsiteUseCase>()
    private val samplePersonas = identifiedArrayListOf(
        sampleDataProvider.samplePersona(IdentityAddress.sampleMainnet().string),
        sampleDataProvider.samplePersona(IdentityAddress.sampleMainnet.random().string)
    )
    private val dApp = DApp.sampleMainnet()
    private val authorizedDapp = Network.AuthorizedDapp(
        networkID = dApp.dAppAddress.networkId.discriminant.toInt(),
        dAppDefinitionAddress = dApp.dAppAddress.string,
        displayName = dApp.name,
        referencesToAuthorizedPersonas = listOf(
            Network.AuthorizedDapp.AuthorizedPersonaSimple(
                identityAddress = samplePersonas[0].address,
                sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData(),
                lastLogin = "2023-01-31T10:28:14Z",
                sharedAccounts = Shared(
                    listOf(AccountAddress.sampleMainnet().string),
                    RequestedNumber(
                        RequestedNumber.Quantifier.AtLeast,
                        1
                    )
                )
            )
        )
    )
    private val dAppConnectionRepository = DAppConnectionRepositoryFake().apply {
        this.savedDApp = authorizedDapp
        state = DAppConnectionRepositoryFake.InitialState.SavedDapp
    }

    override fun initVM(): DappDetailViewModel {
        return DappDetailViewModel(
            dAppConnectionRepository,
            getDAppWithAssociatedResourcesUseCase,
            getValidatedDAppWebsiteUseCase,
            getProfileUseCase,
            incomingRequestRepository,
            savedStateHandle
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        val dApp = DApp.sampleMainnet()
        every { savedStateHandle.get<String>(ARG_DAPP_ADDRESS) } returns dApp.dAppAddress.string
        every { getProfileUseCase() } returns flowOf(
            profile(
                accounts = identifiedArrayListOf(
                    account(address = AccountAddress.sampleMainnet()),
                    account()
                ),
                personas = samplePersonas,
                dApps = listOf(

                )
            )
        )
        coEvery { getDAppWithAssociatedResourcesUseCase(dApp.dAppAddress, false) } returns
                Result.success(DAppWithResources(dApp = dApp))
    }

    @Test
    fun `init load dapp data into state`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.dAppWithResources != null)
            assert(item.dapp != null)
            assert(item.personas.size == 1)
        }
    }

    @Test
    fun `null dapp emission after delete closes screen`() = runTest {
        val vm = vm.value
        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.NoDapp
        advanceUntilIdle()
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is DappDetailEvent.LastPersonaDeleted)
        }
    }

    @Test
    fun `persona click sets persona detail data`() = runTest {
        val expectedPersona = samplePersonas[0].toUiModel()
        val vm = vm.value
        advanceUntilIdle()
        vm.onPersonaClick(samplePersonas[0])
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert((item.selectedSheetState as SelectedSheetState.SelectedPersona).persona == expectedPersona)
            assert(item.sharedPersonaAccounts.size == 1)
        }
    }

    @Test
    fun `persona details closed clear selected persona state`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onPersonaClick(samplePersonas[0])
        advanceUntilIdle()
        vm.onPersonaDetailsClosed()
        advanceUntilIdle()
        vm.state.test {
            val item = expectMostRecentItem()
            assert(item.selectedSheetState == null)
            assert(item.sharedPersonaAccounts.size == 0)
        }
    }

    @Test
    fun `dapp deletion call repo and trigger proper one off event`() = runTest {
        val vm = vm.value
        vm.onDeleteDapp()
        advanceUntilIdle()
        vm.oneOffEvent.test {
            val item = expectMostRecentItem()
            assert(item is DappDetailEvent.DappDeleted)
        }
    }

}*/
