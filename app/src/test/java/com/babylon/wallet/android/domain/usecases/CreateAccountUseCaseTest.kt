package com.babylon.wallet.android.domain.usecases

//class CreateAccountUseCaseTest {
//
//    private val testDispatcher = StandardTestDispatcher()
//    private val testScope = TestScope(testDispatcher)
//
//    private val mnemonicWithPassphrase = MnemonicWithPassphrase(
//        mnemonic = "prison post shoot verb lunch blue limb stick later winner tide roof situate excuse joy muffin cruel fix bag evil call glide resist aware",
//        bip39Passphrase = ""
//    )
//    private val profile = TestData.testProfile2Networks2AccountsEach(mnemonicWithPassphrase)
//    val gateway = Radix.Gateway.hammunet
//    private val profileRepository = Mockito.mock(ProfileRepository::class.java)
//    private val resolveAccountsLedgerStateRepository = mockk<ResolveAccountsLedgerStateRepository>()
//    private val derivationPath = DerivationPath.forAccount(
//        networkId = gateway.network.networkId(),
//        accountIndex = profile.nextAccountIndex(
//            factorSource = TestData.ledgerFactorSource,
//            derivationPathScheme = DerivationPathScheme.CAP_26,
//            forNetworkId = gateway.network.networkId()
//        ),
//        keyType = KeyType.TRANSACTION_SIGNING
//    )
//
//    @Before
//    fun setUp() {
//        coEvery { resolveAccountsLedgerStateRepository(any()) } returns Result.failure(Exception(""))
//    }
//
//    @Test
//    fun `given a account name, a factor source, and a public key with derivation path, when CreateAccountUseCase, then create new account and save it to the profile`() {
//        testScope.runTest {
//            // given
//            val displayName = "A"
//            val factorSource = TestData.ledgerFactorSource
//            val publicKeyAndDerivationPath = AccessFactorSourcesOutput.PublicKeyAndDerivationPath(
//                compressedPublicKey = mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath)
//                    .removeLeadingZero(),
//                derivationPath = derivationPath,
//            )
//
//            // when
//            whenever(profileRepository.profileState).thenReturn(flowOf(ProfileState.Restored(profile)))
//            val createAccountUseCase = CreateAccountUseCase(profileRepository, resolveAccountsLedgerStateRepository)
//            val account = createAccountUseCase.invoke(
//                displayName = displayName,
//                factorSource = factorSource,
//                publicKeyAndDerivationPath = publicKeyAndDerivationPath,
//                onNetworkId = gateway.network.networkId()
//            )
//
//            // then
//            val updatedProfile = profile.addAccounts(
//                accounts = listOf(account),
//                onNetwork = gateway.network.networkId()
//            )
//            verify(profileRepository).saveProfile(updatedProfile)
//        }
//    }
//}