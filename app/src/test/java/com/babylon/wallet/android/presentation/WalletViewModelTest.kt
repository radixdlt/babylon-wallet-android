package com.babylon.wallet.android.presentation

import app.cash.turbine.test
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
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
import rdx.works.core.identifiedArrayListOf
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.BackupState
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WalletViewModelTest : StateViewModelTest<WalletViewModel>() {

    private val getBackupStateUseCase = mockk<GetBackupStateUseCase>()
    private val getWalletAssetsUseCase = mockk<GetWalletAssetsUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getAccountsForSecurityPromptUseCase = mockk<GetEntitiesWithSecurityPromptUseCase>()
    private val ensureBabylonFactorSourceExistUseCase = mockk<EnsureBabylonFactorSourceExistUseCase>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val appEventBus = mockk<AppEventBus>()

    private val sampleProfile = profile(accounts = identifiedArrayListOf(account(address = "adr_1", name = "primary")))
    private val sampleXrdResource = Resource.FungibleResource(
        resourceAddress = XrdResource.address(),
        ownedAmount = BigDecimal.TEN,
        symbolMetadataItem = SymbolMetadataItem(XrdResource.SYMBOL)
    )

    override fun initVM(): WalletViewModel = WalletViewModel(
        getWalletAssetsUseCase,
        getProfileUseCase,
        getAccountsForSecurityPromptUseCase,
        appEventBus,
        ensureBabylonFactorSourceExistUseCase,
        preferencesManager,
        getBackupStateUseCase
    )

    override fun setUp() {
        super.setUp()
        coEvery { ensureBabylonFactorSourceExistUseCase.babylonFactorSourceExist() } returns true
        every { getAccountsForSecurityPromptUseCase() } returns flow { emit(emptyList()) }
        every { getBackupStateUseCase() } returns flowOf(BackupState.Closed)
        every { getProfileUseCase() } returns flowOf(sampleProfile)
        every { appEventBus.events } returns MutableSharedFlow()
        every { preferencesManager.isRadixBannerVisible } returns flowOf(false)
    }

    @Test
    fun `when view model init, view model will fetch accounts & resources`() = runTest {
        val viewModel = vm.value
        advanceUntilIdle()
        coEvery {
            getWalletAssetsUseCase(
                accounts = sampleProfile.currentNetwork.accounts,
                isRefreshing = false
            )
        } returns flowOf(
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
                WalletUiState(isBackupWarningVisible = true, factorSources = sampleProfile.factorSources),
                expectMostRecentItem()
            )
        }
    }
}
