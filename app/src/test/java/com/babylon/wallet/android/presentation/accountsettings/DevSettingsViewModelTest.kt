package com.babylon.wallet.android.presentation.accountsettings

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.account.settings.ARG_ACCOUNT_SETTINGS_ADDRESS
import com.babylon.wallet.android.presentation.account.settings.devsettings.DevSettingsViewModel
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.AddAuthSigningFactorInstanceUseCase

internal class DevSettingsViewModelTest : StateViewModelTest<DevSettingsViewModel>() {

    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val addAuthSigningFactorInstanceUseCase = mockk<AddAuthSigningFactorInstanceUseCase>()
    private val transactionStatusClient = mockk<TransactionStatusClient>()
    private val rolaClient = mockk<ROLAClient>()
    private val sampleProfile = sampleDataProvider.sampleProfile()
    private val sampleAddress = AccountAddress.init(sampleProfile.currentNetwork!!.accounts.first().address)

    override fun initVM(): DevSettingsViewModel {
        return DevSettingsViewModel(
            getProfileUseCase,
            rolaClient,
            incomingRequestRepository,
            addAuthSigningFactorInstanceUseCase,
            transactionStatusClient,
            savedStateHandle
        )
    }

    @Before
    override fun setUp() {
        super.setUp()
        every { getProfileUseCase() } returns flowOf(sampleDataProvider.sampleProfile())
        every { savedStateHandle.get<String>(ARG_ACCOUNT_SETTINGS_ADDRESS) } returns sampleAddress.string
        every { rolaClient.signingState } returns emptyFlow()
    }
}
