package com.babylon.wallet.android.presentation.accountsettings

//@OptIn(ExperimentalCoroutinesApi::class)
//internal class AccountSettingsViewModelTest : StateViewModelTest<AccountSettingsViewModel>() {
//
//    private val getFreeXrdUseCase = mockk<GetFreeXrdUseCase>()
//    private val savedStateHandle = mockk<SavedStateHandle>()
//    private val getProfileUseCase = mockk<GetProfileUseCase>()
//    private val renameAccountDisplayNameUseCase = mockk<RenameAccountDisplayNameUseCase>()
//    private val changeEntityVisibilityUseCase = mockk<ChangeEntityVisibilityUseCase>()
//    private val sampleProfile = sampleDataProvider.sampleProfile()
//    private val sampleAddress = AccountAddress.init(sampleProfile.currentNetwork!!.accounts.first().address)
//    private val eventBus = mockk<AppEventBus>()
//    private val sampleTxId = "txId1"
//
//    override fun initVM(): AccountSettingsViewModel {
//        return AccountSettingsViewModel(
//            getFreeXrdUseCase,
//            getProfileUseCase,
//            renameAccountDisplayNameUseCase,
//            savedStateHandle,
//            changeEntityVisibilityUseCase,
//            TestScope(),
//            eventBus
//        )
//    }
//
//    @Before
//    override fun setUp() {
//        super.setUp()
//        every { getFreeXrdUseCase.getFaucetState(any()) } returns flowOf(FaucetState.Available(true))
//        coEvery { getFreeXrdUseCase(any()) } returns Result.success(sampleTxId)
//        every { getProfileUseCase() } returns flowOf(sampleDataProvider.sampleProfile())
//        every { savedStateHandle.get<String>(ARG_ACCOUNT_SETTINGS_ADDRESS) } returns sampleAddress.string
//        coEvery { changeEntityVisibilityUseCase.hideAccount(any()) } just Runs
//        coEvery { eventBus.sendEvent(any()) } just Runs
//    }
//
//    @Test
//    fun `hide account sets proper state and fire one-off event`() = runTest {
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.onHideAccount()
//        advanceUntilIdle()
//        coVerify(exactly = 1) { changeEntityVisibilityUseCase.hideAccount(any()) }
//        vm.oneOffEvent.test {
//            val item = expectMostRecentItem()
//            assert(item is Event.AccountHidden)
//        }
//    }
//
//    @Test
//    fun `account rename valid & invalid`() = runTest {
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.onRenameAccountNameChange("new valid name")
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.isNewNameValid)
//        }
//        vm.onRenameAccountNameChange("new name very very very very very very very very very very very long")
//        vm.state.test {
//            val item = expectMostRecentItem()
//            assert(item.isNewNameValid.not())
//            assert(item.isNewNameLengthMoreThanTheMaximum)
//        }
//    }
//
//    @Test
//    fun `initial state is correct when free xrd enabled`() = runTest {
//        val vm = vm.value
//        advanceUntilIdle()
//        val state = vm.state.first()
//        assert(state.faucetState is FaucetState.Available)
//    }
//
//    @Test
//    fun `initial state is correct when free xrd not enabled`() = runTest {
//        every { getFreeXrdUseCase.getFaucetState(any()) } returns flow { emit(FaucetState.Available(false)) }
//        val vm = vm.value
//        advanceUntilIdle()
//        val state = vm.state.first()
//        assert(state.faucetState is FaucetState.Available && !(state.faucetState as FaucetState.Available).isEnabled)
//    }
//
//    @Test
//    fun `get free xrd success sets proper state`() = runTest {
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.onGetFreeXrdClick()
//        advanceUntilIdle()
//        coVerify(exactly = 1) { eventBus.sendEvent(AppEvent.RefreshResourcesNeeded) }
//        coVerify(exactly = 1) { getFreeXrdUseCase(sampleAddress) }
//    }
//
//    @Test
//    fun `get free xrd failure sets proper state`() = runTest {
//        coEvery { getFreeXrdUseCase(any()) } returns Result.failure(Exception())
//        val vm = vm.value
//        advanceUntilIdle()
//        vm.onGetFreeXrdClick()
//        advanceUntilIdle()
//        val state = vm.state.first()
//        coVerify(exactly = 1) { getFreeXrdUseCase(sampleAddress) }
//        assert(state.error != null)
//    }
//}
