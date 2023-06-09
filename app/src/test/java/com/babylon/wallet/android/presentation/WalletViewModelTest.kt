package com.babylon.wallet.android.presentation

import app.cash.turbine.test
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.usecases.GetAccountsForSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.wallet.WalletUiState
import com.babylon.wallet.android.presentation.wallet.WalletViewModel
import com.babylon.wallet.android.utils.AppEventBus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import rdx.works.profile.data.model.BackupState
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WalletViewModelTest : StateViewModelTest<WalletViewModel>() {

    private val getAccountsWithResourcesUseCase = mockk<GetAccountsWithResourcesUseCase>()
    private val getBackupStateUseCase = mockk<GetBackupStateUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getAccountsForSecurityPromptUseCase = mockk<GetAccountsForSecurityPromptUseCase>()
    private val appEventBus = mockk<AppEventBus>()

    private val sampleProfile = profile(accounts = listOf(account(address = "adr_1", name = "primary")))
    private val sampleXrdResource = Resource.FungibleResource(
        resourceAddress = "addr_xrd",
        amount = BigDecimal.TEN,
        symbolMetadataItem = SymbolMetadataItem("XRD")
    )
    override fun initVM(): WalletViewModel = WalletViewModel(
        getAccountsWithResourcesUseCase,
        getProfileUseCase,
        getAccountsForSecurityPromptUseCase,
        appEventBus,
        getBackupStateUseCase
    )

    override fun setUp() {
        super.setUp()
        every { getAccountsForSecurityPromptUseCase() } returns flow { emit(emptyList()) }
        every { getBackupStateUseCase() } returns flowOf(BackupState.Closed)
        every { getProfileUseCase() } returns flowOf(sampleProfile)
        every { appEventBus.events } returns MutableSharedFlow()
    }

    @Test
    fun `when view model init, view model will fetch accounts & resources`() = runTest {
        val viewModel = vm.value
        advanceUntilIdle()
        coEvery {
            getAccountsWithResourcesUseCase(
                accounts = sampleProfile.currentNetwork.accounts,
                isRefreshing = false
            )
        } returns Result.Success(
            listOf(
                AccountWithResources(
                    account = sampleProfile.currentNetwork.accounts[0],
                    resources = Resources(fungibleResources = listOf(sampleXrdResource), nonFungibleResources = emptyList())
                ),
                AccountWithResources(
                    account = sampleProfile.currentNetwork.accounts[0],
                    resources = Resources(fungibleResources = emptyList(), nonFungibleResources = emptyList())
                )
            )
        )

        viewModel.state.test {
            assertEquals(
                WalletUiState(isSettingsWarningVisible = true),
                expectMostRecentItem()
            )
        }
    }
}
