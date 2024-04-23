package rdx.works.profile.domain

//internal class EnsureBabylonFactorSourceExistUseCaseTest {
//    private val profileRepository = mockk<ProfileRepository>()
//    private val mnemonicRepository = mockk<MnemonicRepository>()
//    private val deviceInfoRepository = mockk<DeviceInfoRepository>()
//    private val preferenceManager = mockk<PreferencesManager>()
//
//    private val ensureBabylonFactorSourceExistUseCase =
//        EnsureBabylonFactorSourceExistUseCase(mnemonicRepository, profileRepository, deviceInfoRepository, preferenceManager)
//
//    @Before
//    fun setUp() {
//        coEvery { preferenceManager.markFactorSourceBackedUp(any()) } just Runs
//        every { deviceInfoRepository.getDeviceInfo() } returns DeviceInfo("device1", "manufacturer1", "model1")
//        coEvery { mnemonicRepository() } returns MnemonicWithPassphrase(
//            mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
//                    "humble limb repeat video sudden possible story mask neutral prize goose mandate",
//            bip39Passphrase = ""
//        )
//        val profile = Profile.init(
//            id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D",
//            deviceInfo = DeviceInfo(
//                name = "unit",
//                manufacturer = "",
//                model = "test"
//            ),
//            creationDate = Instant.EPOCH
//        )
//        every { profileRepository.profileState } returns flowOf(ProfileState.Restored(profile))
//        coEvery { profileRepository.saveProfile(any()) } just Runs
//    }
//
//    @Test
//    fun `babylon factor source is added to profile if it does not exist`() = runTest {
//        val profile = ensureBabylonFactorSourceExistUseCase()
//        every { profileRepository.profileState } returns flowOf(ProfileState.Restored(profile))
//        ensureBabylonFactorSourceExistUseCase()
//        assert(profile.factorSources.size == 1)
//    }
//}
