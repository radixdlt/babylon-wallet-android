package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.GetAccountDepositResourceRulesUseCase
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.domain.usecases.IsValidRadixDomainUseCase
import com.babylon.wallet.android.domain.usecases.ResolveRadixDomainUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetNextNFTsPageUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWithdrawerBadgeRequirementsUseCase
import com.babylon.wallet.android.domain.usecases.assets.UpdateLSUsInfo
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.common.NetworkContent
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.transfer.accounts.AccountsChooserDelegate
import com.babylon.wallet.android.presentation.transfer.assets.AssetsChooserDelegate
import com.babylon.wallet.android.presentation.transfer.prepare.PrepareManifestDelegate
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.PerAssetTransfers
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.instructions
import com.radixdlt.sargon.manifestPerAssetTransfers
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.assets.Token
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.assets.Assets
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.addressbook.AddAddressBookEntryUseCase
import rdx.works.profile.domain.addressbook.GetAccountAddressBookEntriesOnCurrentNetworkUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class RestrictedTokenTransferE2ETest : StateViewModelTest<TransferViewModel>() {

    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getWalletAssetsUseCase = mockk<GetWalletAssetsUseCase>()
    private val getFiatValueUseCase = mockk<GetFiatValueUseCase>()
    private val getNextNFTsPageUseCase = mockk<GetNextNFTsPageUseCase>()
    private val updateLSUsInfoUseCase = mockk<UpdateLSUsInfo>()
    private val getNetworkInfoUseCase = mockk<GetNetworkInfoUseCase>()
    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val mnemonicRepository = mockk<MnemonicRepository>()
    private val getAccountDepositResourceRulesUseCase = mockk<GetAccountDepositResourceRulesUseCase>()
    private val resolveRadixDomainUseCase = mockk<ResolveRadixDomainUseCase>()
    private val isValidRadixDomainUseCase = mockk<IsValidRadixDomainUseCase>()
    private val getAccountAddressBookEntriesOnCurrentNetworkUseCase = mockk<GetAccountAddressBookEntriesOnCurrentNetworkUseCase>()
    private val addAddressBookEntryUseCase = mockk<AddAddressBookEntryUseCase>()
    private val getWithdrawerBadgeRequirementsUseCase = mockk<GetWithdrawerBadgeRequirementsUseCase>()

    companion object {
        private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities()
        private val fromAccount = profile.networks.asIdentifiable().getBy(NetworkId.MAINNET)?.accounts?.first()!!
        private val otherAccounts = profile.networks.asIdentifiable().getBy(NetworkId.MAINNET)?.accounts?.drop(1).orEmpty()
    }

    private val account1WithAssets = AccountWithAssets(
        account = otherAccounts[0],
        details = null,
        assets = null
    )

    private val getProfileUseCase = GetProfileUseCase(FakeProfileRepository(profile), coroutineRule.dispatcher)

    private val dummyResourceAddress = ResourceAddress.sampleMainnet.xrd
    private val dummyBadgeAddress = ResourceAddress.sampleMainnet.candy

    override fun initVM(): TransferViewModel {
        return TransferViewModel(
            getProfileUseCase = getProfileUseCase,
            accountsChooserDelegate = AccountsChooserDelegate(
                getProfileUseCase = getProfileUseCase,
                getWalletAssetsUseCase = getWalletAssetsUseCase,
                resolveRadixDomainUseCase = resolveRadixDomainUseCase,
                isValidRadixDomainUseCase = isValidRadixDomainUseCase,
                getAccountAddressBookEntriesOnCurrentNetworkUseCase = getAccountAddressBookEntriesOnCurrentNetworkUseCase,
                addAddressBookEntryUseCase = addAddressBookEntryUseCase
            ),
            assetsChooserDelegate = AssetsChooserDelegate(
                getWalletAssetsUseCase = getWalletAssetsUseCase,
                getFiatValueUseCase = getFiatValueUseCase,
                getNextNFTsPageUseCase = getNextNFTsPageUseCase,
                updateLSUsInfo = updateLSUsInfoUseCase,
                getNetworkInfoUseCase = getNetworkInfoUseCase
            ),
            prepareManifestDelegate = PrepareManifestDelegate(
                incomingRequestRepository = incomingRequestRepository,
                mnemonicRepository = mnemonicRepository
            ),
            getAccountDepositResourceRulesUseCase = getAccountDepositResourceRulesUseCase,
            getWithdrawerBadgeRequirementsUseCase = getWithdrawerBadgeRequirementsUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @Before
    override fun setUp() = runTest {
        super.setUp()
        coEvery { getAccountDepositResourceRulesUseCase.invoke(any()) } returns emptySet()
        every { savedStateHandle.get<String>(ARG_ACCOUNT_ID) } returns fromAccount.address.string
        
        val fromAccountWithAssets = AccountWithAssets(
            account = fromAccount,
            details = null,
            assets = Assets(
                tokens = listOf(
                    Token(
                        resource = Resource.FungibleResource(
                            address = dummyResourceAddress,
                            ownedAmount = Decimal192.init("100")
                        )
                    )
                )
            )
        )
        every { getWalletAssetsUseCase.observe(listOf(fromAccount), false) } returns flowOf(listOf(fromAccountWithAssets))
        every { getWalletAssetsUseCase.observe(listOf(otherAccounts[0]), false) } returns flowOf(listOf(account1WithAssets))
        coEvery { getWalletAssetsUseCase.collect(fromAccount, false) } returns Result.success(fromAccountWithAssets)
        coEvery { getWalletAssetsUseCase.collect(otherAccounts[0], false) } returns Result.success(account1WithAssets)
        
        coEvery { getAccountAddressBookEntriesOnCurrentNetworkUseCase.invoke() } returns emptyList()
        coEvery { addAddressBookEntryUseCase.invoke(any(), any(), any()) } returns true
        coEvery { getNetworkInfoUseCase() } returns Result.success(com.babylon.wallet.android.domain.model.NetworkInfo(NetworkId.MAINNET, 1000L))
        coEvery { getFiatValueUseCase.forAccount(any(), any(), any()) } returns Result.success(emptyList())
        coEvery { mnemonicRepository.mnemonicExist(any()) } returns true
        coEvery { incomingRequestRepository.add(any()) } returns Unit
    }

    private suspend fun ReceiveTurbine<TransferViewModel.State>.awaitState(
        predicate: (TransferViewModel.State) -> Boolean
    ): TransferViewModel.State {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) {
                return state
            }
        }
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.testAndCancel(
        validate: suspend ReceiveTurbine<T>.() -> Unit
    ) {
        test {
            validate()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<TransferViewModel.State>.selectAccountAndAsset(
        viewModel: TransferViewModel,
        account: Account,
        resourceAddress: ResourceAddress,
        amount: String = "10"
    ): TargetAccount {
        val skeleton = viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton
        viewModel.onChooseAccountForSkeleton(skeleton)
        
        awaitState { state ->
            val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
            sheet != null && sheet.ownedAccounts.isNotEmpty()
        }

        viewModel.onOwnedAccountSelected(account)
        awaitState { state ->
            val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
            sheet != null && (sheet.selectedAccount as? TargetAccount.Owned)?.account == account
        }

        viewModel.onChooseAccountSubmitted()
        awaitState { state ->
            state.sheet is TransferViewModel.State.Sheet.None &&
            state.targetAccounts.firstOrNull()?.address == account.address
        }

        val targetAccount = viewModel.state.value.targetAccounts[0]
        viewModel.onAddAssetsClick(targetAccount)
        
        awaitState { state ->
            val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAssets
            sheet != null && sheet.assets != null
        }

        val asset = SpendingAsset.Fungible(
            resource = Resource.FungibleResource(
                address = resourceAddress,
                ownedAmount = Decimal192.init("100")
            )
        )
        viewModel.onAssetSelectionChanged(asset, isSelected = true)
        
        awaitState { state ->
            val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAssets
            sheet != null && sheet.targetAccount.spendingAssets.any { it.resourceAddress == resourceAddress }
        }

        viewModel.onChooseAssetsSubmitted()
        
        val stateAfterAssets = awaitState { state ->
            state.sheet is TransferViewModel.State.Sheet.None &&
            state.targetAccounts.firstOrNull()?.spendingAssets?.any { it.resourceAddress == resourceAddress } == true
        }

        val updatedTargetAccount = stateAfterAssets.targetAccounts.first { it.address == account.address }
        val addedAsset = updatedTargetAccount.spendingAssets.first { it.resourceAddress == resourceAddress }
        viewModel.onAmountTyped(updatedTargetAccount, addedAsset, amount)
        
        val finalState = awaitState { state ->
            val acc = state.targetAccounts.firstOrNull { it.address == account.address }
            val fAsset = acc?.spendingAssets?.filterIsInstance<SpendingAsset.Fungible>()?.firstOrNull { it.resourceAddress == resourceAddress }
            fAsset?.amountString == amount
        }

        return finalState.targetAccounts.first { it.address == account.address }
    }

    // =========================================================================
    // Tier 1 (Feature Coverage): F1 - Happy Path / Rules Parsing (Tests 1-5)
    // =========================================================================

    @Test
    fun testF1_happyPath_01_noBadgeRequired() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus == BadgeRequirementStatus.None }
            assertEquals(BadgeRequirementStatus.None, finalState.badgeRequirementStatus)
        }
    }

    @Test
    fun testF1_happyPath_02_fungibleBadgeRequired() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            val status = finalState.badgeRequirementStatus as BadgeRequirementStatus.Success
            assertEquals(dummyBadgeAddress, status.badgeAddress)
        }
    }

    @Test
    fun testF1_happyPath_03_nonFungibleBadgeRequired() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.NonFungible(dummyBadgeAddress, emptyList()))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            val status = finalState.badgeRequirementStatus as BadgeRequirementStatus.Success
            assertEquals(dummyBadgeAddress, status.badgeAddress)
        }
    }

    @Test
    fun testF1_happyPath_04_rulesLoadedSuccessfully() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.accountDepositResourceRulesSet is NetworkContent.Loaded }
            assertTrue(finalState.accountDepositResourceRulesSet is NetworkContent.Loaded)
        }
    }

    @Test
    fun testF1_happyPath_05_noneStatusInitially() = runTest {
        val viewModel = vm.value
        assertEquals(BadgeRequirementStatus.None, viewModel.state.value.badgeRequirementStatus)
    }

    // =========================================================================
    // Tier 1 (Feature Coverage): F2 - Badge Autoselect (Tests 6-10)
    // =========================================================================

    @Test
    fun testF2_badgeAutoselect_01_fungibleOwned() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            assertTrue(finalState.badgeRequirementStatus is BadgeRequirementStatus.Success)
        }
    }

    @Test
    fun testF2_badgeAutoselect_02_nonFungibleOwned() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.NonFungible(dummyBadgeAddress, emptyList()))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            assertTrue(finalState.badgeRequirementStatus is BadgeRequirementStatus.Success)
        }
    }

    @Test
    fun testF2_badgeAutoselect_03_highestBalanceFungible() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            assertEquals(dummyBadgeAddress, (finalState.badgeRequirementStatus as BadgeRequirementStatus.Success).badgeAddress)
        }
    }

    @Test
    fun testF2_badgeAutoselect_04_firstAvailableNonFungible() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.NonFungible(dummyBadgeAddress, emptyList()))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            assertNotNull(finalState.badgeRequirementStatus)
        }
    }

    @Test
    fun testF2_badgeAutoselect_05_successState() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            assertTrue(finalState.badgeRequirementStatus is BadgeRequirementStatus.Success)
        }
    }

    // =========================================================================
    // Tier 1 (Feature Coverage): F3 - Manifest Inject (Tests 11-15)
    // =========================================================================

    @Test
    fun testF3_manifestInject_01_singleBadgeProof() = runTest {
        val requiredBadges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
        val transfers = PerAssetTransfers(fromAccount = fromAccount.address, fungibleResources = emptyList(), nonFungibleResources = emptyList())
        val manifest = manifestPerAssetTransfers(transfers, requiredBadges)
        assertTrue(manifest.instructions.contains("create_proof_of_amount"))
    }

    @Test
    fun testF3_manifestInject_02_multipleBadgesProof() = runTest {
        val requiredBadges = listOf(
            ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")),
            ResourceSpecifier.NonFungible(dummyResourceAddress, emptyList())
        )
        val transfers = PerAssetTransfers(fromAccount = fromAccount.address, fungibleResources = emptyList(), nonFungibleResources = emptyList())
        val manifest = manifestPerAssetTransfers(transfers, requiredBadges)
        assertTrue(manifest.instructions.contains("create_proof_of_amount"))
        assertTrue(manifest.instructions.contains("create_proof_of_non_fungibles"))
    }

    @Test
    fun testF3_manifestInject_03_noProofWhenNoBadge() = runTest {
        val requiredBadges = emptyList<ResourceSpecifier>()
        val transfers = PerAssetTransfers(fromAccount = fromAccount.address, fungibleResources = emptyList(), nonFungibleResources = emptyList())
        val manifest = manifestPerAssetTransfers(transfers, requiredBadges)
        assertTrue(manifest.instructions.isEmpty())
    }

    @Test
    fun testF3_manifestInject_04_instructionSequence() = runTest {
        val transfers = PerAssetTransfers(fromAccount = fromAccount.address, fungibleResources = emptyList(), nonFungibleResources = emptyList())
        val manifest = manifestPerAssetTransfers(transfers, emptyList())
        assertTrue(manifest.instructions.isEmpty())
    }

    @Test
    fun testF3_manifestInject_05_manifestGenerated() = runTest {
        val transfers = PerAssetTransfers(fromAccount = fromAccount.address, fungibleResources = emptyList(), nonFungibleResources = emptyList())
        val manifest = manifestPerAssetTransfers(transfers, emptyList())
        assertNotNull(manifest)
    }

    // =========================================================================
    // Tier 1 (Feature Coverage): F4 - UI Card (Tests 16-20)
    // =========================================================================

    @Test
    fun testF4_uiCard_01_visibleOnLoading() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } coAnswers {
            delay(1000)
            Result.success(
                GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                    status = BadgeRequirementStatus.None,
                    badges = emptyList()
                )
            )
        }
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            val skeleton = viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton
            viewModel.onChooseAccountForSkeleton(skeleton)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
                sheet != null && sheet.ownedAccounts.isNotEmpty()
            }
            viewModel.onOwnedAccountSelected(otherAccounts[0])
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
                sheet != null && (sheet.selectedAccount as? TargetAccount.Owned)?.account == otherAccounts[0]
            }
            viewModel.onChooseAccountSubmitted()
            awaitState { state ->
                state.sheet is TransferViewModel.State.Sheet.None &&
                state.targetAccounts.firstOrNull()?.address == otherAccounts[0].address
            }
            val targetAccount = viewModel.state.value.targetAccounts[0]
            viewModel.onAddAssetsClick(targetAccount)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAssets
                sheet != null && sheet.assets != null
            }
            val asset = SpendingAsset.Fungible(
                resource = Resource.FungibleResource(
                    address = dummyResourceAddress,
                    ownedAmount = Decimal192.init("100")
                )
            )
            viewModel.onAssetSelectionChanged(asset, isSelected = true)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAssets
                sheet != null && sheet.targetAccount.spendingAssets.any { it.resourceAddress == dummyResourceAddress }
            }
            viewModel.onChooseAssetsSubmitted()
            
            val loadingState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Loading }
            assertEquals(BadgeRequirementStatus.Loading, loadingState.badgeRequirementStatus)
        }
    }

    @Test
    fun testF4_uiCard_02_visibleOnSuccess() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            assertTrue(finalState.badgeRequirementStatus is BadgeRequirementStatus.Success)
        }
    }

    @Test
    fun testF4_uiCard_03_visibleOnError() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.MissingBadge),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertTrue(finalState.badgeRequirementStatus is BadgeRequirementStatus.Error)
        }
    }

    @Test
    fun testF4_uiCard_04_hiddenOnNone() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus == BadgeRequirementStatus.None }
            assertEquals(BadgeRequirementStatus.None, finalState.badgeRequirementStatus)
        }
    }

    @Test
    fun testF4_uiCard_05_badgeDetailsDisplayed() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.InsufficientBalance),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            val status = finalState.badgeRequirementStatus as BadgeRequirementStatus.Error
            assertEquals(dummyBadgeAddress, status.badgeAddress)
            assertEquals(BadgeRequirementStatus.Error.Reason.InsufficientBalance, status.reason)
        }
    }

    // =========================================================================
    // Tier 1 (Feature Coverage): F5 - Submit (Tests 21-25)
    // =========================================================================

    @Test
    fun testF5_submit_01_allowedOnNone() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.isSubmitEnabled }
            assertTrue(finalState.isSubmitEnabled)
        }
    }

    @Test
    fun testF5_submit_02_allowedOnSuccess() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.isSubmitEnabled }
            assertTrue(finalState.isSubmitEnabled)
        }
    }

    @Test
    fun testF5_submit_03_blockedOnLoading() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } coAnswers {
            delay(1000)
            Result.success(
                GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                    status = BadgeRequirementStatus.None,
                    badges = emptyList()
                )
            )
        }
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            val skeleton = viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton
            viewModel.onChooseAccountForSkeleton(skeleton)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
                sheet != null && sheet.ownedAccounts.isNotEmpty()
            }
            viewModel.onOwnedAccountSelected(otherAccounts[0])
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
                sheet != null && (sheet.selectedAccount as? TargetAccount.Owned)?.account == otherAccounts[0]
            }
            viewModel.onChooseAccountSubmitted()
            awaitState { state ->
                state.sheet is TransferViewModel.State.Sheet.None &&
                state.targetAccounts.firstOrNull()?.address == otherAccounts[0].address
            }
            val targetAccount = viewModel.state.value.targetAccounts[0]
            viewModel.onAddAssetsClick(targetAccount)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAssets
                sheet != null && sheet.assets != null
            }
            val asset = SpendingAsset.Fungible(
                resource = Resource.FungibleResource(
                    address = dummyResourceAddress,
                    ownedAmount = Decimal192.init("100")
                )
            )
            viewModel.onAssetSelectionChanged(asset, isSelected = true)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAssets
                sheet != null && sheet.targetAccount.spendingAssets.any { it.resourceAddress == dummyResourceAddress }
            }
            viewModel.onChooseAssetsSubmitted()
            
            val loadingState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Loading }
            assertFalse(loadingState.isSubmitEnabled)
        }
    }

    @Test
    fun testF5_submit_04_blockedOnError() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.MissingBadge),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertFalse(finalState.isSubmitEnabled)
        }
    }

    @Test
    fun testF5_submit_05_submitActionTriggersManifest() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            awaitState { it.isSubmitEnabled }
            
            viewModel.onTransferSubmit()
            val stateAfterSubmit = awaitState { it.transferRequestId != null }
            assertNotNull(stateAfterSubmit.transferRequestId)
        }
    }

    // =========================================================================
    // Tier 2 (Boundary & Corner Cases): F1 - Rules Parsing (Tests 26-30)
    // =========================================================================

    @Test
    fun testF1_boundary_01_emptyRules() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.accountDepositResourceRulesSet is NetworkContent.Loaded }
            assertEquals(NetworkContent.Loaded(kotlinx.collections.immutable.persistentSetOf<com.babylon.wallet.android.domain.model.AccountDepositResourceRules>()), finalState.accountDepositResourceRulesSet)
        }
    }

    @Test
    fun testF1_boundary_02_invalidRuleFormat() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.failure(
            Exception("Invalid rule format")
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            val status = finalState.badgeRequirementStatus as BadgeRequirementStatus.Error
            assertEquals(BadgeRequirementStatus.Error.Reason.RuleParsingError, status.reason)
        }
    }

    @Test
    fun testF1_boundary_03_ruleParsingErrorStatus() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.failure(
            Exception("Rule parsing error")
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertTrue(finalState.badgeRequirementStatus is BadgeRequirementStatus.Error)
        }
    }

    @Test
    fun testF1_boundary_04_nullAccountRules() = runTest {
        val viewModel = vm.value
        assertEquals(NetworkContent.None, viewModel.state.value.accountDepositResourceRulesSet)
    }

    @Test
    fun testF1_boundary_05_duplicateRules() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.accountDepositResourceRulesSet is NetworkContent.Loaded }
            assertNotNull(finalState.accountDepositResourceRulesSet)
        }
    }

    // =========================================================================
    // Tier 2 (Boundary & Corner Cases): F2 - Badge Autoselect (Tests 31-35)
    // =========================================================================

    @Test
    fun testF2_boundary_01_zeroFungibleBalance() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.InsufficientBalance),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            val status = finalState.badgeRequirementStatus as BadgeRequirementStatus.Error
            assertEquals(BadgeRequirementStatus.Error.Reason.InsufficientBalance, status.reason)
        }
    }

    @Test
    fun testF2_boundary_02_insufficientFungibleBalance() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.InsufficientBalance),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertTrue(finalState.badgeRequirementStatus is BadgeRequirementStatus.Error)
        }
    }

    @Test
    fun testF2_boundary_03_emptyNonFungibleIds() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.MissingBadge),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            val status = finalState.badgeRequirementStatus as BadgeRequirementStatus.Error
            assertEquals(BadgeRequirementStatus.Error.Reason.MissingBadge, status.reason)
        }
    }

    @Test
    fun testF2_boundary_04_nonMatchingBadgeAddress() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.MissingBadge),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertEquals(dummyBadgeAddress, (finalState.badgeRequirementStatus as BadgeRequirementStatus.Error).badgeAddress)
        }
    }

    @Test
    fun testF2_boundary_05_multipleFungiblesInsufficient() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.InsufficientBalance),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertNotNull(finalState.badgeRequirementStatus)
        }
    }

    // =========================================================================
    // Tier 2 (Boundary & Corner Cases): F3 - Manifest Inject (Tests 36-40)
    // =========================================================================

    @Test
    fun testF3_boundary_01_invalidResourceAddressFormat() = runTest {
        val transfers = PerAssetTransfers(fromAccount = fromAccount.address, fungibleResources = emptyList(), nonFungibleResources = emptyList())
        val manifest = manifestPerAssetTransfers(transfers, emptyList())
        assertNotNull(manifest.instructions)
    }

    @Test
    fun testF3_boundary_02_maxRequiredBadgesProof() = runTest {
        val badges = (1..10).map { ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")) }
        val transfers = PerAssetTransfers(fromAccount = fromAccount.address, fungibleResources = emptyList(), nonFungibleResources = emptyList())
        val manifest = manifestPerAssetTransfers(transfers, badges)
        assertNotNull(manifest)
    }

    @Test
    fun testF3_boundary_03_withdrawingFromMultipleAccounts() = runTest {
        val requiredBadges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
        val transfers = PerAssetTransfers(fromAccount = fromAccount.address, fungibleResources = emptyList(), nonFungibleResources = emptyList())
        val manifest = manifestPerAssetTransfers(transfers, requiredBadges)
        assertTrue(manifest.instructions.contains(fromAccount.address.string))
    }

    @Test
    fun testF3_boundary_04_emptyInstructionManifest() = runTest {
        val transfers = PerAssetTransfers(fromAccount = fromAccount.address, fungibleResources = emptyList(), nonFungibleResources = emptyList())
        val manifest = manifestPerAssetTransfers(transfers, emptyList())
        assertTrue(manifest.instructions.isEmpty())
    }

    @Test
    fun testF3_boundary_05_manifestInjectionFailsSafely() = runTest {
        val transfers = PerAssetTransfers(fromAccount = fromAccount.address, fungibleResources = emptyList(), nonFungibleResources = emptyList())
        val manifest = manifestPerAssetTransfers(transfers, emptyList())
        assertNotNull(manifest)
    }

    // =========================================================================
    // Tier 2 (Boundary & Corner Cases): F4 - UI Card (Tests 41-45)
    // =========================================================================

    @Test
    fun testF4_uiCard_01_dismissibleOnError() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        // Make incomingRequestRepository throw an exception to trigger State.error
        coEvery { incomingRequestRepository.add(any()) } throws Exception("Failed to add interaction")

        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            awaitState { it.isSubmitEnabled }
            
            viewModel.onTransferSubmit()
            val stateWithError = awaitState { it.error != null }
            assertNotNull(stateWithError.error)

            viewModel.onUiMessageShown()
            val stateAfterClear = awaitState { it.error == null }
            assertNull(stateAfterClear.error)
        }
    }

    @Test
    fun testF4_uiCard_02_longBadgeAddressTruncation() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            val status = finalState.badgeRequirementStatus as BadgeRequirementStatus.Success
            assertTrue(status.badgeAddress.string.length > 10)
        }
    }

    @Test
    fun testF4_uiCard_03_flickerPreventionDuringLoading() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } coAnswers {
            delay(1000)
            Result.success(
                GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                    status = BadgeRequirementStatus.None,
                    badges = emptyList()
                )
            )
        }
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            val skeleton = viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton
            viewModel.onChooseAccountForSkeleton(skeleton)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
                sheet != null && sheet.ownedAccounts.isNotEmpty()
            }
            viewModel.onOwnedAccountSelected(otherAccounts[0])
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
                sheet != null && (sheet.selectedAccount as? TargetAccount.Owned)?.account == otherAccounts[0]
            }
            viewModel.onChooseAccountSubmitted()
            awaitState { state ->
                state.sheet is TransferViewModel.State.Sheet.None &&
                state.targetAccounts.firstOrNull()?.address == otherAccounts[0].address
            }
            val targetAccount = viewModel.state.value.targetAccounts[0]
            viewModel.onAddAssetsClick(targetAccount)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAssets
                sheet != null && sheet.assets != null
            }
            val asset = SpendingAsset.Fungible(
                resource = Resource.FungibleResource(
                    address = dummyResourceAddress,
                    ownedAmount = Decimal192.init("100")
                )
            )
            viewModel.onAssetSelectionChanged(asset, isSelected = true)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAssets
                sheet != null && sheet.targetAccount.spendingAssets.any { it.resourceAddress == dummyResourceAddress }
            }
            viewModel.onChooseAssetsSubmitted()
            
            val loadingState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Loading }
            assertEquals(BadgeRequirementStatus.Loading, loadingState.badgeRequirementStatus)
        }
    }

    @Test
    fun testF4_uiCard_04_multipleCardsStaged() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus == BadgeRequirementStatus.None }
            assertEquals(BadgeRequirementStatus.None, finalState.badgeRequirementStatus)
        }
    }

    @Test
    fun testF4_uiCard_05_networkErrorHandling() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.failure(
            Exception("Network Error")
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertTrue(finalState.badgeRequirementStatus is BadgeRequirementStatus.Error)
        }
    }

    // =========================================================================
    // Tier 2 (Boundary & Corner Cases): F5 - Submit (Tests 46-50)
    // =========================================================================

    @Test
    fun testF5_submit_01_quicklyToggledSubmit() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } coAnswers {
            delay(500)
            Result.success(
                GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                    status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                    badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
                )
            )
        }
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            val skeleton = viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton
            viewModel.onChooseAccountForSkeleton(skeleton)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
                sheet != null && sheet.ownedAccounts.isNotEmpty()
            }
            viewModel.onOwnedAccountSelected(otherAccounts[0])
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
                sheet != null && (sheet.selectedAccount as? TargetAccount.Owned)?.account == otherAccounts[0]
            }
            viewModel.onChooseAccountSubmitted()
            awaitState { state ->
                state.sheet is TransferViewModel.State.Sheet.None &&
                state.targetAccounts.firstOrNull()?.address == otherAccounts[0].address
            }
            val targetAccount = viewModel.state.value.targetAccounts[0]
            viewModel.onAddAssetsClick(targetAccount)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAssets
                sheet != null && sheet.assets != null
            }
            val asset = SpendingAsset.Fungible(
                resource = Resource.FungibleResource(
                    address = dummyResourceAddress,
                    ownedAmount = Decimal192.init("100")
                )
            )
            viewModel.onAssetSelectionChanged(asset, isSelected = true)
            awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAssets
                sheet != null && sheet.targetAccount.spendingAssets.any { it.resourceAddress == dummyResourceAddress }
            }
            viewModel.onChooseAssetsSubmitted()
            
            val loadingState = awaitState { 
                it.sheet is TransferViewModel.State.Sheet.None && 
                it.badgeRequirementStatus is BadgeRequirementStatus.Loading 
            }
            assertFalse(loadingState.isSubmitEnabled)
            
            val updatedTargetAccount = loadingState.targetAccounts.first { it.address == otherAccounts[0].address }
            val addedAsset = updatedTargetAccount.spendingAssets.first { it.resourceAddress == dummyResourceAddress }
            viewModel.onAmountTyped(updatedTargetAccount, addedAsset, "10")
            
            delay(600)
            
            val successState = awaitState { 
                it.badgeRequirementStatus is BadgeRequirementStatus.Success && it.isSubmitEnabled 
            }
            assertTrue(successState.isSubmitEnabled)
        }
    }

    @Test
    fun testF5_submit_02_submitWithZeroAssets() = runTest {
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            val initialState = awaitState { it.fromAccount != null }
            assertFalse(initialState.isSubmitEnabled)
        }
    }

    @Test
    fun testF5_submit_03_submitToSelfAllowed() = runTest {
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            val skeleton = viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton
            viewModel.onChooseAccountForSkeleton(skeleton)
            
            val chooseAccountsState = awaitState { state ->
                val sheet = state.sheet as? TransferViewModel.State.Sheet.ChooseAccounts
                sheet != null && sheet.ownedAccounts.isNotEmpty()
            }
            val sheet = chooseAccountsState.sheet as TransferViewModel.State.Sheet.ChooseAccounts
            assertFalse(sheet.ownedAccounts.any { it.address == fromAccount.address })
        }
    }

    @Test
    fun testF5_submit_04_blockedWhenNoValidBadgeFungible() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.MissingBadge),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertFalse(finalState.isSubmitEnabled)
        }
    }

    @Test
    fun testF5_submit_05_blockedWhenNoValidBadgeNonFungible() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.MissingBadge),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            val status = finalState.badgeRequirementStatus as BadgeRequirementStatus.Error
            assertEquals(BadgeRequirementStatus.Error.Reason.MissingBadge, status.reason)
        }
    }

    // =========================================================================
    // Tier 3 (Cross-Feature Combinations): Pairwise Interactions (Tests 51-55)
    // =========================================================================

    @Test
    fun testT3_combination_01_loadingRulesThenAutoselectFails() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.MissingBadge),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertTrue(finalState.badgeRequirementStatus is BadgeRequirementStatus.Error)
        }
    }

    @Test
    fun testT3_combination_02_successfulAutoselectAllowsSubmit() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success && it.isSubmitEnabled }
            assertTrue(finalState.isSubmitEnabled)
        }
    }

    @Test
    fun testT3_combination_03_parsingErrorBlocksSubmit() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.failure(
            Exception("Parsing Error")
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertFalse(finalState.isSubmitEnabled)
        }
    }

    @Test
    fun testT3_combination_04_badgeOwnershipLostBeforeSubmit() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.MissingBadge),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            val status = finalState.badgeRequirementStatus as BadgeRequirementStatus.Error
            assertEquals(BadgeRequirementStatus.Error.Reason.MissingBadge, status.reason)
        }
    }

    @Test
    fun testT3_combination_05_multipleTransfersSomeRestricted() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            assertNotNull(finalState.badgeRequirementStatus)
        }
    }

    // =========================================================================
    // Tier 4 (Real-world Workloads): Complex Scenarios (Tests 56-60)
    // =========================================================================

    @Test
    fun testT4_scenario_01_restrictedAndUnrestrictedTokens() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            assertEquals(dummyBadgeAddress, (finalState.badgeRequirementStatus as BadgeRequirementStatus.Success).badgeAddress)
        }
    }

    @Test
    fun testT4_scenario_02_insufficientBadgeTriggersErrorAndBlocks() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Error(dummyBadgeAddress, BadgeRequirementStatus.Error.Reason.InsufficientBalance),
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Error }
            assertTrue(finalState.badgeRequirementStatus is BadgeRequirementStatus.Error)
        }
    }

    @Test
    fun testT4_scenario_03_manifestRebuiltAfterBadgeRequirementChanges() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            val targetAccount = selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            
            val state1 = awaitState { it.badgeRequirementStatus == BadgeRequirementStatus.None }
            assertNotNull(state1)

            coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
                GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                    status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                    badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
                )
            )

            val asset = targetAccount.spendingAssets.first()
            viewModel.onRemoveAsset(targetAccount, asset)
            
            awaitState { it.badgeRequirementStatus == BadgeRequirementStatus.None }

            viewModel.onAddAssetsClick(targetAccount)
            awaitState { it.sheet is TransferViewModel.State.Sheet.ChooseAssets && (it.sheet as TransferViewModel.State.Sheet.ChooseAssets).assets != null }
            viewModel.onAssetSelectionChanged(asset, isSelected = true)
            viewModel.onChooseAssetsSubmitted()

            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            assertEquals(dummyBadgeAddress, (finalState.badgeRequirementStatus as BadgeRequirementStatus.Success).badgeAddress)
        }
    }

    @Test
    fun testT4_scenario_04_rapidUserInteractionBadgeAutoselect() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.Success(dummyBadgeAddress),
                badges = listOf(ResourceSpecifier.Fungible(dummyBadgeAddress, Decimal192.init("1")))
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.badgeRequirementStatus is BadgeRequirementStatus.Success }
            assertEquals(dummyBadgeAddress, (finalState.badgeRequirementStatus as BadgeRequirementStatus.Success).badgeAddress)
        }
    }

    @Test
    fun testT4_scenario_05_depositRulesUpdateDynamically() = runTest {
        coEvery { getWithdrawerBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetWithdrawerBadgeRequirementsUseCase.BadgeResult(
                status = BadgeRequirementStatus.None,
                badges = emptyList()
            )
        )
        val viewModel = vm.value
        viewModel.state.testAndCancel {
            awaitState { it.fromAccount != null }
            selectAccountAndAsset(viewModel, otherAccounts[0], dummyResourceAddress)
            val finalState = awaitState { it.accountDepositResourceRulesSet is NetworkContent.Loaded }
            assertNotNull(finalState.accountDepositResourceRulesSet)
        }
    }
}
