package rdx.works.profile.domain.account

//internal class MigrateOlympiaAccountsUseCaseTest {
//
//    private val profileRepository = mockk<ProfileRepository>()
//    private val mnemonicRepository = mockk<MnemonicRepository>()
//    private val testDispatcher = StandardTestDispatcher()
//    private val testScope = TestScope(testDispatcher)
//
//    @Test
//    fun `migrate and add accounts to profile`() = testScope.runTest {
//        val olympiaMnemonic = MnemonicWithPassphrase.init(
//            phrase = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote"
//        )
//
//        val factorSource = DeviceFactorSource.olympia(mnemonicWithPassphrase = olympiaMnemonic)
//        val profile = Profile.init(deviceFactorSource = factorSource, creatingDeviceName = TestData.deviceInfo.displayName)
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
//            factorSources = identifiedArrayListOf(DeviceFactorSource.olympia(mnemonicWithPassphrase = olympiaMnemonic)),
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
//                                            publicKey = FactorInstance.PublicKey.curveSecp256k1PublicKey("")
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
//        coEvery { mnemonicRepository.readMnemonic(any()) } returns Result.success(olympiaMnemonic)
//        coEvery { mnemonicRepository.saveMnemonic(any(), any()) } just Runs
//        coEvery { profileRepository.profileState } returns flowOf(ProfileState.Restored(profile))
//        coEvery { profileRepository.saveProfile(any()) } just Runs
//
//        val usecase = MigrateOlympiaAccountsUseCase(profileRepository, testDispatcher)
//        val capturedProfile = slot<Profile>()
//        usecase(getOlympiaTestAccounts(), FactorSource.FactorSourceID.FromHash(
//            kind = FactorSourceKind.DEVICE,
//            body = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5")
//        ))
//        coVerify(exactly = 1) { profileRepository.saveProfile(capture(capturedProfile)) }
//        assert(capturedProfile.captured.currentNetwork!!.accounts.size == 12)
//    }
//
//    private fun getOlympiaTestAccounts(): List<OlympiaAccountDetails> {
//        val words = MnemonicWords("bridge easily outer film record undo turtle method knife quarter promote arch")
//        val seed = words.toSeed(passphrase = "")
//        val accounts = (0..10).map { index ->
//            val derivationPath = DerivationPath.forLegacyOlympia(accountIndex = index)
//            val publicKey = seed.toKey(derivationPath.path, EllipticCurveType.Secp256k1).keyPair.getCompressedPublicKey()
//
//            val olympiaAddress = LegacyOlympiaAccountAddress.init(com.radixdlt.sargon.PublicKey.Secp256k1.init(publicKey.toBagOfBytes()))
//            OlympiaAccountDetails(
//                index = index,
//                type = if (index % 2 == 0) OlympiaAccountType.Software else OlympiaAccountType.Hardware,
//                address = olympiaAddress,
//                publicKey = publicKey.toHexString(),
//                accountName = "Olympia $index",
//                derivationPath = derivationPath,
//                newBabylonAddress = olympiaAddress.toBabylonAddress(),
//                appearanceId = index
//            )
//        }
//        return accounts
//    }
//}
