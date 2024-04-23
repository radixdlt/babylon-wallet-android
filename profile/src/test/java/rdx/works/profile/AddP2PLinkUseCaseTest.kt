package rdx.works.profile

//class AddP2PLinkUseCaseTest {
//
//    @Ignore("P2PLink data class or this unit test needs refactor")
//    @Test
//    fun `given profile exists, when adding p2p client, verify it is added properly`() = runBlocking {
//        val profileRepository = mock(ProfileRepository::class.java)
//        val addP2PLinkUseCase = AddP2PLinkUseCase(profileRepository)
//        val expectedP2PLink = P2PLink.init(
//            connectionPassword = "pass1234",
//            displayName = "Mac browser"
//        )
//
//        val initialProfile = Profile(
//            header = Header.init(
//                id = "9958f568-8c9b-476a-beeb-017d1f843266",
//                deviceInfo = TestData.deviceInfo,
//                creationDate = InstantGenerator(),
//                numberOfNetworks = 0
//            ),
//            appPreferences = AppPreferences(
//                transaction = Transaction.default,
//                display = Display.default,
//                security = Security.default,
//                gateways = Gateways(Radix.Gateway.hammunet.url, listOf(Radix.Gateway.hammunet)),
//                p2pLinks = emptyList()
//            ),
//            factorSources = emptyIdentifiedArrayList(),
//            networks = emptyList()
//        )
//        whenever(profileRepository.profile).thenReturn(flowOf(initialProfile))
//
//        addP2PLinkUseCase(
//            displayName = "Mac browser",
//            connectionPassword = "pass1234"
//        )
//
//        val updatedProfile = initialProfile.copy(
//            appPreferences = initialProfile.appPreferences.copy(
//                p2pLinks = listOf(expectedP2PLink)
//            )
//        )
//        verify(profileRepository).saveProfile(updatedProfile)
//    }
//}
