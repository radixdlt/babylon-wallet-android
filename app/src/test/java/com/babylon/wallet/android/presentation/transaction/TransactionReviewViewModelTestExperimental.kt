package com.babylon.wallet.android.presentation.transaction

//@OptIn(ExperimentalCoroutinesApi::class)
//internal class TransactionReviewViewModelTestExperimental : StateViewModelTest<TransactionReviewViewModel>(
//    testDispatcherRule = TestDispatcherRule(dispatcher = UnconfinedTestDispatcher())
//) {
//
//    @get:Rule
//    val defaultLocaleTestRule = DefaultLocaleRule()
//
//    private val transactionId = UUID.randomUUID().toString()
//    private val testProfile = Profile.sample()
//
//    private val savedStateHandle = mockk<SavedStateHandle>().apply {
//        every { get<String>(ARG_TRANSACTION_REQUEST_ID) } returns transactionId
//    }
//    private val emptyExecutionSummary = ExecutionSummary(
//        feeLocks = FeeLocks(
//            lock = 0.toDecimal192(),
//            contingentLock = 0.toDecimal192()
//        ),
//        feeSummary = FeeSummary(
//            executionCost = 0.toDecimal192(),
//            finalizationCost = 0.toDecimal192(),
//            storageExpansionCost = 0.toDecimal192(),
//            royaltyCost = 0.toDecimal192()
//        ),
//        detailedClassification = listOf(),
//        reservedInstructions = listOf(),
//        deposits = mapOf(),
//        withdrawals = mapOf(),
//        addressesOfAccountsRequiringAuth = listOf(),
//        addressesOfIdentitiesRequiringAuth = listOf(),
//        encounteredAddresses = listOf(),
//        newEntities = NewEntities(
//            metadata = mapOf()
//        ),
//        presentedProofs = listOf(),
//        newlyCreatedNonFungibles = listOf()
//    )
//    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
//    private val transactionRepository = mockk<TransactionRepository>().apply {
//        coEvery { getLedgerEpoch() } returns Result.success(1000.toULong())
//        coEvery { analyzeTransaction(any(), any(), any()) } returns Result.success(
//            TransactionToReviewData(
//                transactionToReview = TransactionToReview(
//                    transactionManifest = TransactionManifest.sample(),
//                    executionSummary = emptyExecutionSummary
//                ),
//                message = Message.None
//            )
//        )
//    }
//    private val stateRepository = mockk<StateRepository>()
//    private val respondToIncomingRequestUseCase = mockk<RespondToIncomingRequestUseCase>()
//    private val appEventBus = AppEventBusImpl()
//    private val exceptionMessageProvider = mockk<ExceptionMessageProvider>()
//    private val signTransactionUseCase = mockk<SignTransactionUseCase>()
//    private val preferencesManager = mockk<PreferencesManager>()
//    private val getFiatValueUseCase = mockk<GetFiatValueUseCase>()
//
//    private val profileRepository = FakeProfileRepository(profile = testProfile)
//    private val testScope = TestScope(context = coroutineRule.dispatcher)
//
//    private val getProfileUseCase = GetProfileUseCase(profileRepository, coroutineRule.dispatcher)
//
//    override fun initVM(): TransactionReviewViewModel = testViewModel(
//        transactionRepository = transactionRepository,
//        incomingRequestRepository = incomingRequestRepository,
//        signTransactionUseCase = signTransactionUseCase,
//        profileRepository = profileRepository,
//        stateRepository = stateRepository,
//        respondToIncomingRequestUseCase = respondToIncomingRequestUseCase,
//        appEventBus = appEventBus,
//        preferencesManager = preferencesManager,
//        exceptionMessageProvider = exceptionMessageProvider,
//        savedStateHandle = savedStateHandle,
//        testScope = testScope,
//        getProfileUseCase = getProfileUseCase,
//        testDispatcher = coroutineRule.dispatcher,
//        getFiatValueUseCase = getFiatValueUseCase
//    )
//
//    @Test
//    fun `given transaction id, when this id does not exist in the queue, then dismiss the transaction`() = runTest {
//        every { incomingRequestRepository.getRequest(transactionId) } returns null
//        vm.value.oneOffEvent.test {
//            assertTrue(awaitItem() is TransactionReviewViewModel.Event.Dismiss)
//        }
//    }
//
//    @Ignore("Not ready yet")
//    @Test
//    fun `transaction approval success`() = runTest {
//        mockManifestInput(manifestData = simpleXRDTransfer(testProfile))
//        coEvery {
//            stateRepository.getOwnedXRD(testProfile.networks.asIdentifiable().getBy(NetworkId.MAINNET)?.accounts.orEmpty())
//        } returns Result.success(testProfile.networks.asIdentifiable().getBy(NetworkId.MAINNET)?.accounts?.associateWith { 10.toDecimal192() }.orEmpty())
//        val notarization = NotarizationResult(
//            intentHash = TransactionIntentHash.sample(),
//            endEpoch = 0u,
//            notarizedTransaction = NotarizedTransaction.sample()
//        )
//        coEvery { signTransactionUseCase(any()) } returns Result.success(notarization)
//        coEvery { transactionRepository.submitTransaction(any()) } returns Result.success(TransactionIntentHash.sample())
//
//
//        vm.value.state.test {
//            println("---------> ${awaitItem()}")
//
//            //vm.value.onPayerSelected(selectedFeePayer = testProfile.networks.first().accounts.first())
//            //println("---------> ${awaitItem()}")
//            //ensureAllEventsConsumed()
//            //vm.value.approveTransaction { true }
//
////            coVerify(exactly = 1) {
////                dAppMessenger.sendTransactionWriteResponseSuccess(
////                    remoteConnectorId = "remoteConnectorId",
////                    requestId = transactionId,
////                    txId = notarisation.txIdHash
////                )
////            }
//        }
//    }
//
//    private fun mockManifestInput(manifestData: UnvalidatedManifestData = sampleManifest(instructions = "")) {
//        val transactionRequest = TransactionRequest(
//            remoteEntityId = RemoteEntityID.ConnectorId("remoteConnectorId"),
//            interactionId = transactionId,
//            unvalidatedManifestData = manifestData,
//            requestMetadata = requestMetadata(manifestData = manifestData)
//        ).also {
//            println(it.unvalidatedManifestData.instructions)
//        }
//        coEvery { incomingRequestRepository.getRequest(transactionId) } returns transactionRequest
//    }
//
//    private fun simpleXRDTransfer(withProfile: Profile): UnvalidatedManifestData =
//        with(withProfile.networks.asIdentifiable().getBy(NetworkId.MAINNET)?.accounts?.first()!!) {
//            UnvalidatedManifestData.from(
//                manifest = TransactionManifest.perRecipientTransfers(
//                    transfers = PerRecipientAssetTransfers(
//                        addressOfSender = address,
//                        transfers = listOf(
//                            PerRecipientAssetTransfer(
//                                recipient = AccountOrAddressOf.ProfileAccount(value = this),
//                                fungibles = listOf(
//                                    PerRecipientFungibleTransfer(
//                                        useTryDepositOrAbort = false,
//                                        amount = 2.toDecimal192(),
//                                        divisibility = null,
//                                        resourceAddress = ResourceAddress.xrd(networkId)
//                                    )
//                                ),
//                                nonFungibles = emptyList()
//                            )
//                        )
//                    )
//                ),
//            )
//        }
//}