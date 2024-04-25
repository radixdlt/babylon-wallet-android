package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

//@OptIn(ExperimentalCoroutinesApi::class)
//internal class PersonaDataOnetimeViewModelTest : StateViewModelTest<PersonaDataOnetimeViewModel>() {
//
//    private val getProfileUseCase = mockk<GetProfileUseCase>()
//    private val savedStateHandle = mockk<SavedStateHandle>()
//    private val preferencesManager = mockk<PreferencesManager>()
//
//    private val samplePersona = sampleDataProvider.samplePersona()
//
//    override fun initVM(): PersonaDataOnetimeViewModel {
//        return PersonaDataOnetimeViewModel(
//            savedStateHandle,
//            getProfileUseCase,
//            preferencesManager
//        )
//    }
//
//    @Before
//    override fun setUp() {
//        super.setUp()
//        every { savedStateHandle.get<RequiredPersonaFields>(ARG_REQUIRED_FIELDS) } returns RequiredPersonaFields(
//            fields = listOf(
//                RequiredPersonaField(
//                    PersonaDataField.Kind.Name,
//                    MessageFromDataChannel.IncomingRequest.NumberOfValues(
//                        1,
//                        MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
//                    )
//                )
//            )
//        )
//        coEvery { preferencesManager.firstPersonaCreated } returns flow {
//            emit(true)
//        }
//        coEvery { getProfileUseCase() } returns flowOf(
//            profile(personas = identifiedArrayListOf(samplePersona))
//        )
//    }
//
//    @Test
//    fun `initial state is set up properly`() = runTest {
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.personaListToDisplay.size == 1)
//            assert(item.personaListToDisplay.first().missingFieldKinds().size == 0)
//        }
//    }
//
//    @Test
//    fun `selecting persona enables continue button`() = runTest {
//        val vm = vm.value
//        vm.onSelectPersona(samplePersona)
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.continueButtonEnabled)
//        }
//    }
//
//    @Test
//    fun `edit click triggers edit action with proper required fields`() = runTest {
//        val vm = vm.value
//        vm.onEditClick(samplePersona.address)
//        advanceUntilIdle()
//        val item = vm.oneOffEvent.first()
//        assert(item is PersonaDataOnetimeEvent.OnEditPersona && item.requiredPersonaFields.fields.any { it.kind == PersonaDataField.Kind.Name })
//    }
//
//}
