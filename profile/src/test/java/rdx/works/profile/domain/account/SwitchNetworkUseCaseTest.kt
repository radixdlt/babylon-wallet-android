package rdx.works.profile.domain.account

//@OptIn(ExperimentalCoroutinesApi::class)
//internal class SwitchNetworkUseCaseTest {
//    private val testDispatcher = StandardTestDispatcher()
//    private val testScope = TestScope(testDispatcher)
//    private val profileRepository = mockk<ProfileRepository>()
//
//    private val useCase = SwitchNetworkUseCase(profileRepository, testDispatcher)
//
//    @Before
//    fun setUp() {
//        val mnemonicWithPassphrase = MnemonicWithPassphrase(
//            mnemonic = "noodle question hungry sail type offer grocery clay nation hello mixture forum",
//            bip39Passphrase = ""
//        )
//        every { profileRepository.profileState } returns flowOf(ProfileState.Restored(TestData.testProfile2Networks2AccountsEach(mnemonicWithPassphrase)))
//        coEvery { profileRepository.saveProfile(any()) } just Runs
//    }
//
//    @Test
//    fun `switching network changes profile current network`() = testScope.runTest {
//        val changedNetworkId = useCase.invoke(Radix.Gateway.kisharnet.url, Radix.Network.kisharnet.id)
//        val updatedProfile = slot<Profile>()
//        coVerify(exactly = 1) { profileRepository.saveProfile(capture(updatedProfile)) }
//        assert(updatedProfile.captured.appPreferences.gateways.current().url == Radix.Gateway.kisharnet.url)
//        assert(changedNetworkId == Radix.Network.kisharnet.networkId())
//    }
//}