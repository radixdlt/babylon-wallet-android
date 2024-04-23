package rdx.works.profile.domain

//internal class AddOlympiaFactorSourceUseCaseTest {
//
//    private val profileRepository = mockk<ProfileRepository>()
//    private val getProfileUseCase = mockk<GetProfileUseCase>()
//    private val mnemonicRepository = mockk<MnemonicRepository>()
//    private val preferencesManager = mockk<PreferencesManager>()
//
//    @Test
//    fun `new factor source is added to a profile, if it does not already exist`() = runTest {
//        val mnemonicWithPassphrase = MnemonicWithPassphrase(
//            mnemonic = "noodle question hungry sail type offer grocery clay nation hello mixture forum",
//            bip39Passphrase = ""
//        )
//        val olympiaMnemonic = MnemonicWithPassphrase(
//            mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote",
//            bip39Passphrase = ""
//        )
//
//        val network = Radix.Gateway.hammunet
//        val profile = Profile(
//            header = Header.init(
//                id = "9958f568-8c9b-476a-beeb-017d1f843266",
//                deviceInfo = TestData.deviceInfo,
//                creationDate = InstantGenerator(),
//                numberOfNetworks = 1,
//                numberOfAccounts = 1
//            ),
//            appPreferences = AppPreferences(
//                transaction = Transaction.default,
//                display = Display.default,
//                security = Security.default,
//                gateways = Gateways(network.url, listOf(network)),
//                p2pLinks = listOf(
//                    P2PLink.init(
//                        connectionPassword = "My password",
//                        displayName = "Browser name test"
//                    )
//                )
//            ),
//            factorSources = identifiedArrayListOf(DeviceFactorSource.babylon(mnemonicWithPassphrase = mnemonicWithPassphrase)),
//            networks = listOf(
//                Network(
//                    accounts = identifiedArrayListOf(
//                        Network.Account(
//                            address = "fj3489fj348f",
//                            appearanceID = 123,
//                            displayName = "my account",
//                            networkID = network.network.networkId().value,
//                            securityState = SecurityState.Unsecured(
//                                unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
//                                    transactionSigning = FactorInstance(
//                                        badge = FactorInstance.Badge.VirtualSource.HierarchicalDeterministic(
//                                            derivationPath = DerivationPath.forAccount(
//                                                networkId = network.network.networkId(),
//                                                accountIndex = 0,
//                                                keyType = KeyType.TRANSACTION_SIGNING
//                                            ),
//                                            publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
//                                        ),
//                                        factorSourceId = FactorSource.FactorSourceID.FromHash(
//                                            kind = FactorSourceKind.DEVICE,
//                                            body = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5")
//                                        )
//                                    )
//                                )
//                            ),
//                            onLedgerSettings = Network.Account.OnLedgerSettings.init()
//                        )
//                    ),
//                    authorizedDapps = emptyList(),
//                    networkID = network.network.networkId().value,
//                    personas = emptyIdentifiedArrayList()
//                )
//            )
//        )
//
//        coEvery { mnemonicRepository.mnemonicExist(any()) } returns false
//        coEvery { mnemonicRepository.saveMnemonic(any(), any()) } just Runs
//        coEvery { profileRepository.profileState } returns flowOf(ProfileState.Restored(profile))
//        coEvery { profileRepository.saveProfile(any()) } just Runs
//        coEvery { preferencesManager.markFactorSourceBackedUp(any()) } just Runs
//        every { getProfileUseCase() } returns flowOf(profile)
//
//        val usecase = AddOlympiaFactorSourceUseCase(getProfileUseCase, profileRepository, mnemonicRepository, preferencesManager)
//        val capturedProfile = slot<Profile>()
//        usecase(olympiaMnemonic)
//        coVerify(exactly = 1) { profileRepository.saveProfile(capture(capturedProfile)) }
//        assert(capturedProfile.captured.factorSources.size == 2)
//
//        coEvery { mnemonicRepository.mnemonicExist(any()) } returns true
//        usecase(olympiaMnemonic)
//        coVerify(exactly = 1) { mnemonicRepository.saveMnemonic(any(), any()) }
//        coVerify(exactly = 1) { profileRepository.saveProfile(any()) }
//    }
//}
