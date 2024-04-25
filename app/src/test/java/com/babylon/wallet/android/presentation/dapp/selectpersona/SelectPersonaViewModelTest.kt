package com.babylon.wallet.android.presentation.dapp.selectpersona

//@OptIn(ExperimentalCoroutinesApi::class)
//internal class SelectPersonaViewModelTest : StateViewModelTest<SelectPersonaViewModel>() {
//
//    private val getProfileUseCase = mockk<GetProfileUseCase>()
//    private val savedStateHandle = mockk<SavedStateHandle>()
//    private val preferencesManager = mockk<PreferencesManager>()
//    private val dAppConnectionRepository = DAppConnectionRepositoryFake()
//
//    override fun initVM(): SelectPersonaViewModel {
//        return SelectPersonaViewModel(
//            savedStateHandle,
//            dAppConnectionRepository,
//            getProfileUseCase,
//            preferencesManager
//        )
//    }
//
//    @Before
//    override fun setUp() {
//        super.setUp()
//        coEvery { preferencesManager.firstPersonaCreated } returns flow {
//            emit(true)
//        }
//        every { savedStateHandle.get<String>(ARG_DAPP_DEFINITION_ADDRESS) } returns DApp.sampleMainnet().dAppAddress.string
//        every { getProfileUseCase() } returns flowOf(
//            profile(
//                personas = identifiedArrayListOf(
//                    SampleDataProvider().samplePersona(IdentityAddress.sampleMainnet().string),
//                    SampleDataProvider().samplePersona(IdentityAddress.sampleMainnet.other().string)
//                )
//            )
//        )
//    }
//
//    @Test
//    fun `connected dapp exist and has authorized persona`() = runTest {
//        dAppConnectionRepository.state = DAppConnectionRepositoryFake.InitialState.PredefinedDapp
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.continueButtonEnabled)
//            assert(item.personaListToDisplay.size == 2)
//            val onePersonaAuthorized = item.personaListToDisplay.count { it.lastUsedOn != null } == 1
//            assert(onePersonaAuthorized)
//        }
//    }
//
//    @Test
//    fun `connected dapp does not exist`() = runTest {
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(!item.continueButtonEnabled)
//            assert(item.personaListToDisplay.size == 2)
//            val noPersonaAuthorized = item.personaListToDisplay.all { it.lastUsedOn == null }
//            assert(noPersonaAuthorized)
//        }
//    }
//
//}
