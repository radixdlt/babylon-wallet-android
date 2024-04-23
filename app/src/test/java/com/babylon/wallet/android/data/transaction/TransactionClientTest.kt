package com.babylon.wallet.android.data.transaction

internal class TransactionClientTest {

//    @get:Rule
//    val coroutineRule = TestDispatcherRule()
//
//    private val transactionRepository = mockk<TransactionRepository>()
//    private val getProfileUseCase = GetProfileUseCase(ProfileRepositoryFake)
//    private val collectSignersSignaturesUseCase = mockk<CollectSignersSignaturesUseCase>()
//    private val resolveNotaryAndSignersUseCase = ResolveNotaryAndSignersUseCase(getProfileUseCase)
//
//    private lateinit var transactionClient: TransactionClient
//
//    @Before
//    fun setUp() {
//        coEvery { collectSignersSignaturesUseCase.interactionState } returns emptyFlow()
//        transactionClient = TransactionClient(
//            transactionRepository,
//            resolveNotaryAndSignersUseCase,
//            collectSignersSignaturesUseCase
//        )
//    }
//
//    @Test
//    fun `when address exists, finds address involved & signing for set metadata manifest`() =
//        runTest {
//            val manifest = manifestWithAddress(account1)
//                .addLockFeeInstructionToManifest(account1.address, TransactionConfig.DEFAULT_LOCK_FEE.toBigDecimal())
//                .summary(networkId = Radix.Gateway.default.network.id.toUByte())
//
//            val notaryKey = PrivateKey.EddsaEd25519.newRandom()
//            val notaryAndSigners = resolveNotaryAndSignersUseCase(manifest, notaryKey)
//
//            Assert.assertEquals(listOf(account1), notaryAndSigners.getOrNull()?.signers)
//        }
//
//    companion object {
//        private val account1 = account(name = "account1", address = "account_rdx12x20vgu94d96g3demdumxl6yjpvm0jy8dhrr03g75299ghxrwq76uh")
//        private val account2 = account(name = "account2", address = "account_rdx12x20vgu94d96g3demdumxl6yjpvm0jy8dhrr03g75299ghxrwq73uh")
//
//        private fun manifestWithAddress(
//            account: Network.Account
//        ): TransactionManifest = sampleXRDWithdraw(
//            fromAddress = account.address,
//            value = BigDecimal.TEN
//        ).toTransactionManifest().getOrThrow()
//
//        private object ProfileRepositoryFake : ProfileRepository {
//            private val profile = profile(accounts = identifiedArrayListOf(account1, account2))
//
//            override val profileState: Flow<ProfileState> = flowOf(ProfileState.Restored(profile = profile))
//
//            override val inMemoryProfileOrNull: Profile?
//                get() = profile
//
//            override suspend fun saveProfile(profile: Profile) {
//                error("Not needed")
//            }
//
//            override suspend fun clearProfileDataOnly() {
//                error("Not needed")
//            }
//
//            override suspend fun clearAllWalletData() {
//                error("Not needed")
//            }
//
//            override fun deriveProfileState(content: String): ProfileState {
//                error("Not needed")
//            }
//        }
//    }
}
