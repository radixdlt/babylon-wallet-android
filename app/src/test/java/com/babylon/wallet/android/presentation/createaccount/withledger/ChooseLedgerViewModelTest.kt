package com.babylon.wallet.android.presentation.createaccount.withledger

//@OptIn(ExperimentalCoroutinesApi::class)
//internal class ChooseLedgerViewModelTest : StateViewModelTest<ChooseLedgerViewModel>() {
//
//    private val getProfileUseCase = mockk<GetProfileUseCase>()
//    private val ledgerMessenger = mockk<LedgerMessenger>()
//    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
//    private val addLedgerFactorSourceUseCase = mockk<AddLedgerFactorSourceUseCase>()
//    private val eventBus = mockk<AppEventBus>()
//    private val savedStateHandle = mockk<SavedStateHandle>()
//
//
//    private val firstDeviceId = "5f47ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010996f5"
//    private val secondDeviceId = "5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5"
//
//    override fun initVM(): ChooseLedgerViewModel {
//        return ChooseLedgerViewModel(
//            getProfileUseCase,
//            eventBus,
//            savedStateHandle
//        )
//    }
//
//    @Before
//    override fun setUp() {
//        super.setUp()
//        coEvery { ledgerMessenger.isAnyLinkedConnectorConnected } returns flowOf(true)
//        coEvery { eventBus.sendEvent(any()) } just Runs
//        coEvery { getProfileUseCase() } returns flowOf(profile())
//        every { savedStateHandle.get<LedgerSelectionPurpose>(ARG_SELECTION_PURPOSE) } returns LedgerSelectionPurpose.DerivePublicKey
//        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway.mainnet
//        coEvery {
//            addLedgerFactorSourceUseCase(
//                HexCoded32Bytes(firstDeviceId),
//                any(),
//                any()
//            )
//        } returns AddLedgerFactorSourceResult.Added(
//            LedgerHardwareWalletFactorSource.newSource(
//                model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
//                name = "ledger",
//                deviceID = HexCoded32Bytes(secondDeviceId)
//            )
//        )
//    }
//
//    @Test
//    fun `initial state is correct with 1 factor source and no p2pLinks`() = runTest {
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.ledgerDevices.size == 1)
//            assert(item.ledgerDevices.first { it.selected }.data.id.body.value == secondDeviceId)
//        }
//    }
//
//    @Test
//    fun `initial state is correct with 1 factor source and 1 p2pLink`() = runTest {
//        coEvery { getProfileUseCase() } returns flowOf(profile(p2pLinks = listOf(P2PLink("pwd", "chrome"))))
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.ledgerDevices.size == 1)
//            assert(item.ledgerDevices.first { it.selected }.data.id.body.value == secondDeviceId)
//        }
//    }
//}
