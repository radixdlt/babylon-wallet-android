package com.babylon.wallet.android.presentation

import app.cash.turbine.test
import com.babylon.wallet.android.NPSSurveyState
import com.babylon.wallet.android.NPSSurveyStateObserver
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
import com.babylon.wallet.android.data.repository.locker.AccountLockerRepository
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.domain.utils.AccountLockersObserver
import com.babylon.wallet.android.presentation.wallet.WalletViewModel
import com.babylon.wallet.android.presentation.wallet.cards.HomeCardsDelegate
import com.babylon.wallet.android.presentation.wallet.locker.WalletAccountLockersDelegate
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import rdx.works.core.InstantGenerator
import rdx.works.core.TimestampGenerator
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.cloudbackup.BackupState
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.profile.cloudbackup.domain.CheckMigrationToNewBackupSystemUseCase
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import rdx.works.profile.domain.display.ChangeBalanceVisibilityUseCase

@ExperimentalCoroutinesApi
class WalletViewModelTest : StateViewModelTest<WalletViewModel>() {

    private val getBackupStateUseCase = mockk<GetBackupStateUseCase>()
    private val getWalletAssetsUseCase = mockk<GetWalletAssetsUseCase>()
    private val getFiatValueUseCase = mockk<GetFiatValueUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getAccountsForSecurityPromptUseCase = mockk<GetEntitiesWithSecurityPromptUseCase>()
    private val ensureBabylonFactorSourceExistUseCase = mockk<EnsureBabylonFactorSourceExistUseCase>()
    private val checkMigrationToNewBackupSystemUseCase = mockk<CheckMigrationToNewBackupSystemUseCase>()
    private val changeBalanceVisibilityUseCase = mockk<ChangeBalanceVisibilityUseCase>()
    private val npsSurveyStateObserver = mockk<NPSSurveyStateObserver>()
    private val appEventBus = mockk<AppEventBus>()
    private val testDispatcher = StandardTestDispatcher()
    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val p2PLinksRepository = mockk<P2PLinksRepository>()
    private val homeCardsRepository = mockk<HomeCardsRepository>()
    private val accountLockersObserver = mockk<AccountLockersObserver>()
    private val accountLockersRepository = mock<AccountLockerRepository>()

    private val sampleProfile = Profile.sample()
    private val sampleXrdResource = Resource.FungibleResource(
        address = XrdResource.address(networkId = NetworkId.MAINNET),
        ownedAmount = 10.toDecimal192(),
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
        npsSurveyStateObserver,
        p2PLinksRepository,
        checkMigrationToNewBackupSystemUseCase,
        testDispatcher,
        HomeCardsDelegate(
            homeCardsRepository
        ),
        WalletAccountLockersDelegate(
            accountLockersObserver,
            accountLockersRepository,
            testDispatcher
        )
    )

    override fun setUp() {
        super.setUp()
        coEvery { incomingRequestRepository.consumeBufferedRequest() } returns null
        coEvery { ensureBabylonFactorSourceExistUseCase.babylonFactorSourceExist() } returns true
        every { getAccountsForSecurityPromptUseCase() } returns flow { emit(emptyList()) }
        every { getBackupStateUseCase() } returns flowOf(
            BackupState.CloudBackupDisabled(
                email = "email",
                lastCloudBackupTime = TimestampGenerator(),
                lastManualBackupTime = InstantGenerator(),
                lastModifiedProfileTime = TimestampGenerator()
            )
        )
        every { getProfileUseCase.flow } returns flowOf(sampleProfile)
        every { appEventBus.events } returns MutableSharedFlow()
        every { npsSurveyStateObserver.npsSurveyState } returns flowOf(NPSSurveyState.InActive)
        coEvery { p2PLinksRepository.showRelinkConnectors() } returns flowOf(false)
        every { homeCardsRepository.observeHomeCards() } returns flowOf(emptyList())
        every { accountLockersObserver.depositsByAccount() } returns flowOf(emptyMap())
    }

    @Test
    fun `when view model init, view model will fetch accounts & resources`() = runTest {
        coEvery { checkMigrationToNewBackupSystemUseCase() } returns false
        val viewModel = vm.value
        advanceUntilIdle()
        val accounts = sampleProfile.networks.asIdentifiable().getBy(NetworkId.MAINNET)?.accounts.orEmpty()
        coEvery {
            getWalletAssetsUseCase(
                accounts = accounts,
                isRefreshing = false
            )
        } returns flowOf(
            listOf(
                AccountWithAssets(
                    account = accounts[0],
                    assets = Assets(
                        tokens = listOf(Token(sampleXrdResource)),
                        nonFungibles = emptyList(),
                        poolUnits = emptyList()
                    )
                ),
                AccountWithAssets(
                    account = accounts[0],
                    assets = Assets(tokens = emptyList(), nonFungibles = emptyList())
                )
            )
        )

        viewModel.state.test {
            assertEquals(
                WalletViewModel.State(),
                expectMostRecentItem()
            )
        }
    }
}
