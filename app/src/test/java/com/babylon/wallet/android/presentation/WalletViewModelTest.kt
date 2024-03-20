package com.babylon.wallet.android.presentation

import app.cash.turbine.test
import com.babylon.wallet.android.NPSSurveyState
import com.babylon.wallet.android.NPSSurveyStateObserver
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
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
import rdx.works.profile.domain.display.ChangeBalanceVisibilityUseCase
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WalletViewModelTest : StateViewModelTest<WalletViewModel>() {

    private val getBackupStateUseCase = mockk<GetBackupStateUseCase>()
    private val getWalletAssetsUseCase = mockk<GetWalletAssetsUseCase>()
    private val getFiatValueUseCase = mockk<GetFiatValueUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getAccountsForSecurityPromptUseCase = mockk<GetEntitiesWithSecurityPromptUseCase>()
    private val ensureBabylonFactorSourceExistUseCase = mockk<EnsureBabylonFactorSourceExistUseCase>()
    private val changeBalanceVisibilityUseCase = mockk<ChangeBalanceVisibilityUseCase>()
    private val npsSurveyStateObserver = mockk<NPSSurveyStateObserver>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val appEventBus = mockk<AppEventBus>()

    private val sampleProfile = profile(accounts = identifiedArrayListOf(account(address = "adr_1", name = "primary")))
    private val sampleXrdResource = Resource.FungibleResource(
        resourceAddress = XrdResource.address(),
        ownedAmount = BigDecimal.TEN,
        metadata = listOf(
            Metadata.Primitive(key = ExplicitMetadataKey.SYMBOL.key, value = XrdResource.SYMBOL, valueType = MetadataType.String)
        )
    )

    override fun initVM(): WalletViewModel = WalletViewModel(
        getWalletAssetsUseCase,
        getFiatValueUseCase,
        getProfileUseCase,
        getAccountsForSecurityPromptUseCase,
        changeBalanceVisibilityUseCase,
        appEventBus,
        ensureBabylonFactorSourceExistUseCase,
        preferencesManager,
        npsSurveyStateObserver,
        getBackupStateUseCase,
    )

    override fun setUp() {
        super.setUp()
        coEvery { ensureBabylonFactorSourceExistUseCase.babylonFactorSourceExist() } returns true
        every { getAccountsForSecurityPromptUseCase() } returns flow { emit(emptyList()) }
        every { getBackupStateUseCase() } returns flowOf(BackupState.Closed)
        every { getProfileUseCase() } returns flowOf(sampleProfile)
        every { appEventBus.events } returns MutableSharedFlow()
        every { preferencesManager.isRadixBannerVisible } returns flowOf(false)
        every { npsSurveyStateObserver.npsSurveyState } returns flowOf(NPSSurveyState.InActive)
    }

    @Test
    fun `when view model init, view model will fetch accounts & resources`() = runTest {
        val viewModel = vm.value
        advanceUntilIdle()
        coEvery {
            getWalletAssetsUseCase(
                accounts = sampleProfile.currentNetwork?.accounts.orEmpty(),
                isRefreshing = false
            )
        } returns flowOf(
            listOf(
                AccountWithAssets(
                    account = sampleProfile.currentNetwork!!.accounts[0],
                    assets = Assets(
                        tokens = listOf(Token(sampleXrdResource)),
                        nonFungibles = emptyList(),
                        poolUnits = emptyList()
                    )
                ),
                AccountWithAssets(
                    account = sampleProfile.currentNetwork!!.accounts[0],
                    assets = Assets(tokens = emptyList(), nonFungibles = emptyList())
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
