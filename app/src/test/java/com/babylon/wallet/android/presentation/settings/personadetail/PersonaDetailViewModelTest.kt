package com.babylon.wallet.android.presentation.settings.personadetail

//@OptIn(ExperimentalCoroutinesApi::class)
//internal class PersonaDetailViewModelTest : StateViewModelTest<PersonaDetailViewModel>() {
//
//    private val dAppConnectionRepository = DAppConnectionRepositoryFake()
//    private val getProfileUseCase = mockk<GetProfileUseCase>()
//    private val savedStateHandle = mockk<SavedStateHandle>()
//    private val getDAppsUseCase = mockk<GetDAppsUseCase>()
//    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
//    private val changeEntityVisibilityUseCase = mockk<ChangeEntityVisibilityUseCase>()
//    private val addAuthSigningFactorInstanceUseCase = mockk<AddAuthSigningFactorInstanceUseCase>()
//    private val rolaClient = mockk<ROLAClient>()
//    private val eventBus = mockk<AppEventBus>()
//
//    override fun initVM(): PersonaDetailViewModel {
//        return PersonaDetailViewModel(
//            dAppConnectionRepository,
//            getProfileUseCase,
//            eventBus,
//            addAuthSigningFactorInstanceUseCase,
//            rolaClient,
//            incomingRequestRepository,
//            getDAppsUseCase,
//            savedStateHandle,
//            changeEntityVisibilityUseCase
//        )
//    }
//
//    @Before
//    override fun setUp() {
//        super.setUp()
//        val identityAddress = IdentityAddress.sampleMainnet()
//        every { savedStateHandle.get<String>(ARG_PERSONA_ADDRESS) } returns identityAddress.string
//        every { getProfileUseCase() } returns flowOf(
//            profile(
//                personas = identifiedArrayListOf(
//                    SampleDataProvider().samplePersona(identityAddress.string)
//                )
//            )
//        )
//        val dApp = DApp.sampleMainnet()
//        val dAppOther = DApp.sampleMainnet.other()
//        coEvery { getDAppsUseCase(dApp.dAppAddress, false) } returns Result.success(dApp)
//        coEvery { getDAppsUseCase(dAppOther.dAppAddress, false) } returns Result.success(dAppOther)
//    }
//
//    @Test
//    fun `init load persona and dapps`() = runTest {
//        every { eventBus.events } returns MutableSharedFlow()
//        val vm = vm.value
//        val collectJob = launch(UnconfinedTestDispatcher()) { vm.state.collect {} }
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.persona?.address == IdentityAddress.sampleMainnet().string)
//            assert(item.authorizedDapps.size == 2)
//        }
//        collectJob.cancel()
//    }
//}
