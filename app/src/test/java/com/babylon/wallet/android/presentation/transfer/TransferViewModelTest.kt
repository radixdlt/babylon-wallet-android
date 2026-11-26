package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.presentation.common.NetworkContent
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.GetAccountDepositResourceRulesUseCase
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.domain.usecases.IsValidRadixDomainUseCase
import com.babylon.wallet.android.domain.usecases.ResolveRadixDomainUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetNextNFTsPageUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetTransferBadgeRequirementsUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.domain.usecases.assets.UpdateLSUsInfo
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.transfer.accounts.AccountsChooserDelegate
import com.babylon.wallet.android.presentation.transfer.assets.AssetsChooserDelegate
import com.babylon.wallet.android.presentation.transfer.prepare.PrepareManifestDelegate
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.extensions.mainnet
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rdx.works.core.domain.resources.Resource
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.addressbook.AddAddressBookEntryUseCase
import rdx.works.profile.domain.addressbook.GetAccountAddressBookEntriesOnCurrentNetworkUseCase

@ExperimentalCoroutinesApi
class TransferViewModelTest : StateViewModelTest<TransferViewModel>() {

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
    private val getTransferBadgeRequirementsUseCase = mockk<GetTransferBadgeRequirementsUseCase>()

    private val profile = Profile.sample().changeGateway(Gateway.mainnet).unHideAllEntities()
    private val fromAccount = profile.networks.asIdentifiable().getBy(NetworkId.MAINNET)?.accounts?.first()!!
    private val otherAccounts = profile.networks.asIdentifiable().getBy(NetworkId.MAINNET)?.accounts?.drop(1).orEmpty()
    private val restrictedToken = ResourceAddress.sampleMainnet.candy
    private val otherToken = ResourceAddress.sampleMainnet.random()
    private val fungibleBadge = ResourceAddress.sampleMainnet.random()
    private val nftBadge = ResourceAddress.sampleMainnet.nonFungibleGCMembership
    private val account1WithAssets = AccountWithAssets(
        account = otherAccounts[0],
        details = null,
        assets = null
    )

    private val getProfileUseCase = GetProfileUseCase(FakeProfileRepository(profile), coroutineRule.dispatcher)

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
            savedStateHandle = savedStateHandle,
            getAccountDepositResourceRulesUseCase = getAccountDepositResourceRulesUseCase,
            getTransferBadgeRequirementsUseCase = getTransferBadgeRequirementsUseCase
        )
    }

    @Before
    override fun setUp() = runTest {
        super.setUp()
        coEvery { getAccountDepositResourceRulesUseCase.invoke(any()) } returns emptySet()
        coEvery { getTransferBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetTransferBadgeRequirementsUseCase.Result(status = BadgeRequirementStatus.None, requiredBadges = emptyList())
        )
        every { savedStateHandle.get<String>(ARG_ACCOUNT_ID) } returns fromAccount.address.string
        every { getWalletAssetsUseCase.observe(listOf(otherAccounts[0]), false) } returns flowOf(listOf(account1WithAssets))
        coEvery { getAccountAddressBookEntriesOnCurrentNetworkUseCase.invoke() } returns emptyList()
        coEvery { addAddressBookEntryUseCase.invoke(any(), any(), any()) } returns true
        // The assets chooser of the sending account
        every { getWalletAssetsUseCase.observe(listOf(fromAccount), false) } returns flowOf(
            listOf(AccountWithAssets(account = fromAccount, details = null, assets = null))
        )
        coEvery { getFiatValueUseCase.forAccount(any(), any(), any()) } returns Result.failure(RuntimeException("No prices"))
        coEvery { getNetworkInfoUseCase.invoke() } returns Result.failure(RuntimeException("No network info"))
        coEvery { mnemonicRepository.mnemonicExist(any()) } returns true
    }

    @Test
    fun `when message changed verify it is reflected in the ui state`() = runTest {
        val viewModel = vm.value
        viewModel.state.test {
            assertFromAccountSet()

            viewModel.onMessageStateChanged(isOpen = true)
            assertEquals(
                awaitItem().messageState,
                TransferViewModel.State.Message.Added(message = "")
            )

            val message = "New Message"
            viewModel.onMessageChanged(message)
            assertEquals(
                awaitItem().messageState,
                TransferViewModel.State.Message.Added(message = message)
            )
        }
    }

    @Test
    fun `choosing an owned account from the accounts chooser`() = runTest {
        val viewModel = vm.value
        viewModel.state.test {
            assertFromAccountSet()
            assertOpenSheetForSkeleton(viewModel, viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton)
            assertSubmittingOwnedAccount(viewModel, otherAccounts[0])
            assert(awaitItem().accountDepositResourceRulesSet is NetworkContent.Loaded)
        }
    }

    @Test
    fun `choosing a third party address from the accounts chooser`() = runTest {
        val viewModel = vm.value
        viewModel.state.test {
            assertFromAccountSet()
            assertOpenSheetForSkeleton(viewModel, viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton)
            assertOtherAccountSubmitted(viewModel, AccountAddress.sampleMainnet.random().string)
            assert(awaitItem().accountDepositResourceRulesSet is NetworkContent.Loaded)
        }
    }

    @Test
    fun `given owned account already chosen, the chooser has less accounts to show`() = runTest {
        val viewModel = vm.value
        viewModel.state.test {
            assertFromAccountSet()
            assertOpenSheetForSkeleton(viewModel, viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton)
            assertSubmittingOwnedAccount(viewModel, otherAccounts[0])
            assert(awaitItem().accountDepositResourceRulesSet is NetworkContent.Loaded)
            viewModel.addAccountClick()
            val secondSkeleton = awaitItem().targetAccounts[1] as TargetAccount.Skeleton
            assertOpenSheetForSkeleton(viewModel, secondSkeleton)
        }
    }

    @Test
    fun `given account already chosen, the user can delete the account`() = runTest {
        val viewModel = vm.value
        viewModel.state.test {
            assertFromAccountSet()
            val initialSkeletonAccount = viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton
            assertOpenSheetForSkeleton(viewModel, initialSkeletonAccount)
            assertSubmittingOwnedAccount(viewModel, otherAccounts[0])
            assert(awaitItem().accountDepositResourceRulesSet is NetworkContent.Loaded)
            viewModel.deleteAccountClick(
                from = TargetAccount.Owned(
                    account = otherAccounts[0],
                    id = initialSkeletonAccount.id
                )
            )
            val resultState = awaitItem()
            assertTrue(resultState.targetAccounts.size == 1)
            assertTrue(resultState.targetAccounts[0] is TargetAccount.Skeleton)
        }
    }

    @Test
    fun `a held fungible badge is proven in the transfer manifest`() = runTest {
        val proof = ResourceSpecifier.Fungible(resourceAddress = fungibleBadge, amount = 1.toDecimal192())
        badgeCheckReturns(BadgeRequirementStatus.Success(resource = null), listOf(proof))
        val viewModel = vm.value

        chooseAssetsToTransfer(viewModel, restrictedToken)
        assertEquals(BadgeRequirementStatus.Success(resource = null), viewModel.state.value.badgeRequirementStatus)
        assertTrue(viewModel.state.value.isSubmitEnabled)

        val instructions = submittedManifestInstructions(viewModel)
        assertTrue(instructions, instructions.contains("create_proof_of_amount"))
        assertTrue(instructions.contains(fungibleBadge.string))
    }

    @Test
    fun `a held NFT badge is proven in the transfer manifest`() = runTest {
        val proof = ResourceSpecifier.NonFungible(resourceAddress = nftBadge, ids = listOf(NonFungibleLocalId.sample()))
        badgeCheckReturns(BadgeRequirementStatus.Success(resource = null), listOf(proof))
        val viewModel = vm.value

        chooseAssetsToTransfer(viewModel, restrictedToken)

        val instructions = submittedManifestInstructions(viewModel)
        assertTrue(instructions, instructions.contains("create_proof_of_non_fungibles"))
        assertTrue(instructions.contains(nftBadge.string))
    }

    @Test
    fun `without restricted tokens the manifest has no proof`() = runTest {
        val viewModel = vm.value

        chooseAssetsToTransfer(viewModel, restrictedToken)
        assertEquals(BadgeRequirementStatus.None, viewModel.state.value.badgeRequirementStatus)
        assertTrue(viewModel.state.value.isSubmitEnabled)

        assertFalse(submittedManifestInstructions(viewModel).contains("create_proof"))
    }

    @Test
    fun `a missing badge disables the transfer`() = runTest {
        val missing = BadgeRequirementStatus.Error(
            resource = null,
            reason = BadgeRequirementStatus.Error.Reason.MissingBadge,
            restrictedResource = restrictedToken
        )
        badgeCheckReturns(missing, emptyList())
        val viewModel = vm.value

        chooseAssetsToTransfer(viewModel, restrictedToken)

        assertEquals(missing, viewModel.state.value.badgeRequirementStatus)
        assertFalse(viewModel.state.value.isSubmitEnabled)
    }

    @Test
    fun `a failed check disables the transfer`() = runTest {
        coEvery { getTransferBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.failure(RuntimeException("No connection"))
        val viewModel = vm.value

        chooseAssetsToTransfer(viewModel, restrictedToken)

        val status = viewModel.state.value.badgeRequirementStatus as BadgeRequirementStatus.Error
        assertEquals(BadgeRequirementStatus.Error.Reason.CheckFailed, status.reason)
        assertTrue(viewModel.state.value.resolvedRequiredBadges.isEmpty())
        assertFalse(viewModel.state.value.isSubmitEnabled)
    }

    @Test
    fun `the transfer is disabled while the badges are checked`() = runTest {
        val check = CompletableDeferred<Result<GetTransferBadgeRequirementsUseCase.Result>>()
        coEvery { getTransferBadgeRequirementsUseCase.invoke(any(), any()) } coAnswers { check.await() }
        val viewModel = vm.value

        chooseAssetsToTransfer(viewModel, restrictedToken)
        assertEquals(BadgeRequirementStatus.Loading, viewModel.state.value.badgeRequirementStatus)
        assertFalse(viewModel.state.value.isSubmitEnabled)

        check.complete(
            Result.success(GetTransferBadgeRequirementsUseCase.Result(status = BadgeRequirementStatus.None, requiredBadges = emptyList()))
        )
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isSubmitEnabled)
    }

    @Test
    fun `changing the assets checks the badges again`() = runTest {
        val viewModel = vm.value

        chooseAssetsToTransfer(viewModel, restrictedToken)
        chooseAssetsToTransfer(viewModel, otherToken)

        coVerify(exactly = 1) { getTransferBadgeRequirementsUseCase.invoke(fromAccount.address, setOf(restrictedToken)) }
        coVerify(exactly = 1) {
            getTransferBadgeRequirementsUseCase.invoke(fromAccount.address, setOf(restrictedToken, otherToken))
        }
    }

    @Test
    fun `retry after a failed check checks the badges again`() = runTest {
        val proof = ResourceSpecifier.Fungible(resourceAddress = fungibleBadge, amount = 1.toDecimal192())
        coEvery { getTransferBadgeRequirementsUseCase.invoke(any(), any()) } returnsMany listOf(
            Result.failure(RuntimeException("No connection")),
            Result.success(
                GetTransferBadgeRequirementsUseCase.Result(
                    status = BadgeRequirementStatus.Success(resource = null),
                    requiredBadges = listOf(proof)
                )
            )
        )
        val viewModel = vm.value

        chooseAssetsToTransfer(viewModel, restrictedToken)
        assertFalse(viewModel.state.value.isSubmitEnabled)

        viewModel.onBadgeRequirementRetry()
        advanceUntilIdle()

        coVerify(exactly = 2) { getTransferBadgeRequirementsUseCase.invoke(fromAccount.address, setOf(restrictedToken)) }
        assertEquals(BadgeRequirementStatus.Success(resource = null), viewModel.state.value.badgeRequirementStatus)
        assertEquals(listOf(proof), viewModel.state.value.resolvedRequiredBadges)
        assertTrue(viewModel.state.value.isSubmitEnabled)
    }

    private fun badgeCheckReturns(status: BadgeRequirementStatus, requiredBadges: List<ResourceSpecifier>) {
        coEvery { getTransferBadgeRequirementsUseCase.invoke(any(), any()) } returns Result.success(
            GetTransferBadgeRequirementsUseCase.Result(status = status, requiredBadges = requiredBadges)
        )
    }

    // Sends 1 of the token to an owned account, through the accounts and assets choosers.
    private fun TestScope.chooseAssetsToTransfer(viewModel: TransferViewModel, token: ResourceAddress) {
        advanceUntilIdle()
        val target = viewModel.state.value.targetAccounts[0]
        if (target is TargetAccount.Skeleton) {
            viewModel.onChooseAccountForSkeleton(target)
            advanceUntilIdle()
            viewModel.onOwnedAccountSelected(otherAccounts[0])
            viewModel.onChooseAccountSubmitted()
            advanceUntilIdle()
        }

        val asset = SpendingAsset.Fungible(
            resource = Resource.FungibleResource(address = token, ownedAmount = 10.toDecimal192())
        )
        viewModel.onAddAssetsClick(viewModel.state.value.targetAccounts[0])
        advanceUntilIdle()
        viewModel.onAssetSelectionChanged(asset, isSelected = true)
        viewModel.onChooseAssetsSubmitted()
        advanceUntilIdle()
        viewModel.onAmountTyped(viewModel.state.value.targetAccounts[0], asset, "1")
        advanceUntilIdle()
    }

    private fun TestScope.submittedManifestInstructions(viewModel: TransferViewModel): String {
        val request = slot<DappToWalletInteraction>()
        coEvery { incomingRequestRepository.add(capture(request)) } just Runs

        viewModel.onTransferSubmit()
        advanceUntilIdle()

        return (request.captured as TransactionRequest).unvalidatedManifestData.instructions
    }

    private suspend fun ReceiveTurbine<TransferViewModel.State>.assertFromAccountSet() {
        assertNull(awaitItem().fromAccount)
        assertEquals(fromAccount, awaitItem().fromAccount)
    }

    private suspend fun ReceiveTurbine<TransferViewModel.State>.assertOpenSheetForSkeleton(
        viewModel: TransferViewModel,
        skeleton: TargetAccount.Skeleton
    ) {
        viewModel.onChooseAccountForSkeleton(skeleton)
        val initialSheet = awaitItem().sheet as TransferViewModel.State.Sheet.ChooseAccounts
        assertEquals(skeleton, initialSheet.selectedAccount)
        assertEquals(persistentListOf<Account>(), initialSheet.ownedAccounts)
        assertFalse(initialSheet.isResolving)

        val remainingAccounts = otherAccounts.filterNot { account ->
            viewModel.state.value.targetAccounts.any { it.address == account.address }
        }
        if (remainingAccounts.isNotEmpty()) {
            val updatedSheet = awaitItem().sheet as TransferViewModel.State.Sheet.ChooseAccounts
            assertEquals(skeleton, updatedSheet.selectedAccount)
            assertEquals(remainingAccounts.toPersistentList(), updatedSheet.ownedAccounts)
            assertFalse(updatedSheet.isResolving)
        }
    }

    private suspend fun ReceiveTurbine<TransferViewModel.State>.assertSubmittingOwnedAccount(
        viewModel: TransferViewModel,
        account: Account
    ) {
        val skeletonAccount = viewModel.state.value.targetAccounts[0]
        viewModel.onOwnedAccountSelected(account)
        assertTrue(
            (awaitItem().sheet as TransferViewModel.State.Sheet.ChooseAccounts).selectedAccount == TargetAccount.Owned(
                account = account,
                id = skeletonAccount.id
            )
        )

        viewModel.onChooseAccountSubmitted()
        awaitItem()
        assertEquals(
            TransferViewModel.State(
                fromAccount = fromAccount,
                targetAccounts = persistentListOf(
                    TargetAccount.Owned(
                        account = account,
                        id = skeletonAccount.id
                    )
                ),
                sheet = TransferViewModel.State.Sheet.None
            ),
            awaitItem()
        )
        awaitItem()
    }

    private suspend fun ReceiveTurbine<TransferViewModel.State>.assertOtherAccountSubmitted(viewModel: TransferViewModel, address: String) {
        val skeletonAccount = viewModel.state.value.targetAccounts[0]
        viewModel.onReceiverChanged(address)

        val sheetState = awaitItem().sheet as TransferViewModel.State.Sheet.ChooseAccounts
        // Check that the address is passed as valid
        assertEquals(
            sheetState.selectedAccount,
            TargetAccount.Other(
                typed = address,
                validity = TargetAccount.Other.InputValidity.VALID,
                resolvedInput = TargetAccount.Other.ResolvedInput.AccountInput(
                    accountAddress = AccountAddress.init(address)
                ),
                id = skeletonAccount.id
            )
        )
        // Check that the owned accounts are disabled for selection
        assertFalse(sheetState.isOwnedAccountsEnabled)

        viewModel.onChooseAccountSubmitted()
        awaitItem()
        assertEquals(
            TransferViewModel.State(
                fromAccount = fromAccount,
                targetAccounts = persistentListOf(
                    TargetAccount.Other(
                        typed = address,
                        validity = TargetAccount.Other.InputValidity.VALID,
                        resolvedInput = TargetAccount.Other.ResolvedInput.AccountInput(
                            accountAddress = AccountAddress.init(address)
                        ),
                        id = skeletonAccount.id
                    )
                ),
                sheet = TransferViewModel.State.Sheet.None
            ),
            awaitItem()
        )
        awaitItem()
    }
}
