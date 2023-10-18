package com.babylon.wallet.android.presentation

import app.cash.turbine.test
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.AccountWithAssets
import com.babylon.wallet.android.domain.model.Assets
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.XrdResource
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.usecases.GetAccountsForSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.GetAccountsWithAssetsUseCase
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
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WalletViewModelTest : StateViewModelTest<WalletViewModel>() {

    private val getAccountsWithAssetsUseCase = mockk<GetAccountsWithAssetsUseCase>()
    private val getBackupStateUseCase = mockk<GetBackupStateUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getAccountsForSecurityPromptUseCase = mockk<GetAccountsForSecurityPromptUseCase>()
    private val ensureBabylonFactorSourceExistUseCase = mockk<EnsureBabylonFactorSourceExistUseCase>()
    private val appEventBus = mockk<AppEventBus>()

    private val sampleProfile = profile(accounts = listOf(account(address = "adr_1", name = "primary")))
    private val sampleXrdResource = Resource.FungibleResource(
        resourceAddress = XrdResource.address(),
        ownedAmount = BigDecimal.TEN,
        symbolMetadataItem = SymbolMetadataItem(XrdResource.SYMBOL)
    )

    override fun initVM(): WalletViewModel = WalletViewModel(
        getAccountsWithAssetsUseCase,
        getProfileUseCase,
        getAccountsForSecurityPromptUseCase,
        appEventBus,
        ensureBabylonFactorSourceExistUseCase,
        getBackupStateUseCase
    )

    override fun setUp() {
        super.setUp()
        coEvery { ensureBabylonFactorSourceExistUseCase.babylonFactorSourceExist() } returns true
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
            getAccountsWithAssetsUseCase(
                accounts = sampleProfile.currentNetwork.accounts,
                isRefreshing = false
            )
        } returns Result.Success(
            listOf(
                AccountWithAssets(
                    account = sampleProfile.currentNetwork.accounts[0],
                    assets = Assets(
                        fungibles = listOf(sampleXrdResource),
                        nonFungibles = emptyList(),
                        poolUnits = emptyList()
                    )
                ),
                AccountWithAssets(
                    account = sampleProfile.currentNetwork.accounts[0],
                    assets = Assets(fungibles = emptyList(), nonFungibles = emptyList())
                )
            )
        )

        viewModel.state.test {
            assertEquals(
                WalletUiState(isSettingsWarningVisible = true, factorSources = sampleProfile.factorSources),
                expectMostRecentItem()
            )
        }
    }
}
