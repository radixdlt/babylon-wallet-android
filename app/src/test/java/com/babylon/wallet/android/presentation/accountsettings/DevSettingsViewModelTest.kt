package com.babylon.wallet.android.presentation.accountsettings

//internal class DevSettingsViewModelTest : StateViewModelTest<DevSettingsViewModel>() {
//
//    private val savedStateHandle = mockk<SavedStateHandle>()
//    private val getProfileUseCase = mockk<GetProfileUseCase>()
//    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
//    private val addAuthSigningFactorInstanceUseCase = mockk<AddAuthSigningFactorInstanceUseCase>()
//    private val transactionStatusClient = mockk<TransactionStatusClient>()
//    private val rolaClient = mockk<ROLAClient>()
//    private val sampleProfile = sampleDataProvider.sampleProfile()
//    private val sampleAddress = AccountAddress.init(sampleProfile.currentNetwork!!.accounts.first().address)
//
//    override fun initVM(): DevSettingsViewModel {
//        return DevSettingsViewModel(
//            getProfileUseCase,
//            rolaClient,
//            incomingRequestRepository,
//            addAuthSigningFactorInstanceUseCase,
//            transactionStatusClient,
//            savedStateHandle
//        )
//    }
//
//    @Before
//    override fun setUp() {
//        super.setUp()
//        every { getProfileUseCase() } returns flowOf(sampleDataProvider.sampleProfile())
//        every { savedStateHandle.get<String>(ARG_ACCOUNT_SETTINGS_ADDRESS) } returns sampleAddress.string
//        every { rolaClient.signingState } returns emptyFlow()
//    }
//}
