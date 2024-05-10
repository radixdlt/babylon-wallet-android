package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetNextNFTsPageUseCase
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
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase

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

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities()
    private val fromAccount = profile.networks.asIdentifiable().getBy(NetworkId.MAINNET)?.accounts?.first()!!
    private val otherAccounts = profile.networks.asIdentifiable().getBy(NetworkId.MAINNET)?.accounts?.drop(1).orEmpty()
    private val account1WithAssets = AccountWithAssets(
        account = otherAccounts[0],
        details = null,
        assets = null
    )
    private val getProfileUseCase = GetProfileUseCase(FakeProfileRepository(profile))

    override fun initVM(): TransferViewModel {
        return TransferViewModel(
            getProfileUseCase = getProfileUseCase,
            accountsChooserDelegate = AccountsChooserDelegate(
                getProfileUseCase = getProfileUseCase,
                getWalletAssetsUseCase = getWalletAssetsUseCase
            ),
            assetsChooserDelegate = AssetsChooserDelegate(
                getWalletAssetsUseCase = getWalletAssetsUseCase,
                getFiatValueUseCase =getFiatValueUseCase,
                getNextNFTsPageUseCase = getNextNFTsPageUseCase,
                updateLSUsInfo = updateLSUsInfoUseCase,
                getNetworkInfoUseCase = getNetworkInfoUseCase
            ),
            prepareManifestDelegate = PrepareManifestDelegate(
                incomingRequestRepository = incomingRequestRepository,
                mnemonicRepository = mnemonicRepository
            ),
            savedStateHandle = savedStateHandle
        )
    }

    @Before
    override fun setUp() = runTest {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_ACCOUNT_ID) } returns fromAccount.address.string
        every { getWalletAssetsUseCase(listOf(otherAccounts[0]), false) } returns flowOf(listOf(account1WithAssets))
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
        }
    }

    @Test
    fun `choosing a third party address from the accounts chooser`() = runTest {
        val viewModel = vm.value
        viewModel.state.test {
            assertFromAccountSet()
            assertOpenSheetForSkeleton(viewModel, viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton)
            assertOtherAccountSubmitted(viewModel, AccountAddress.sampleMainnet.random().string)
        }
    }

    @Test
    fun `given owned account already chosen, the chooser has less accounts to show`() = runTest {
        val viewModel = vm.value
        viewModel.state.test {
            assertFromAccountSet()
            assertOpenSheetForSkeleton(viewModel, viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton)
            assertSubmittingOwnedAccount(viewModel, otherAccounts[0])

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

            viewModel.deleteAccountClick(from = TargetAccount.Owned(
                account = otherAccounts[0],
                id = initialSkeletonAccount.id
            ))
            val resultState = awaitItem()
            assertTrue(resultState.targetAccounts.size == 1)
            assertTrue(resultState.targetAccounts[0] is TargetAccount.Skeleton)
        }
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
        assertEquals(
            TransferViewModel.State.Sheet.ChooseAccounts(
                selectedAccount = skeleton,
                ownedAccounts = persistentListOf(),
                isLoadingAssetsForAccount = false
            ),
            awaitItem().sheet
        )

        val remainingAccounts = otherAccounts.filterNot { account ->
            viewModel.state.value.targetAccounts.any { it.address == account.address }
        }
        if (remainingAccounts.isNotEmpty()) {
            assertEquals(
                TransferViewModel.State.Sheet.ChooseAccounts(
                    selectedAccount = skeleton,
                    ownedAccounts = remainingAccounts.toPersistentList(),
                    isLoadingAssetsForAccount = false
                ),
                awaitItem().sheet
            )
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
    }

    private suspend fun ReceiveTurbine<TransferViewModel.State>.assertOtherAccountSubmitted(viewModel: TransferViewModel, address: String) {
        val skeletonAccount = viewModel.state.value.targetAccounts[0]
        viewModel.onAddressTyped(address)

        val sheetState = awaitItem().sheet as TransferViewModel.State.Sheet.ChooseAccounts
        // Check that the address is passed as valid
        assertEquals(
            sheetState.selectedAccount,
             TargetAccount.Other(
                typedAddress = address,
                validity = TargetAccount.Other.AddressValidity.VALID,
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
                        typedAddress = address,
                        validity = TargetAccount.Other.AddressValidity.VALID,
                        id = skeletonAccount.id
                    )
                ),
                sheet = TransferViewModel.State.Sheet.None
            ),
            awaitItem()
        )
    }
}

