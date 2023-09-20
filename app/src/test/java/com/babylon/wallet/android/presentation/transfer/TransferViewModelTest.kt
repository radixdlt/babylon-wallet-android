package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.utils.AppEventBus
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.CheckMnemonicIntegrityUseCase
import rdx.works.profile.domain.GetProfileUseCase

@ExperimentalCoroutinesApi
class TransferViewModelTest : StateViewModelTest<TransferViewModel>() {

    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getAccountsWithResourcesUseCase = mockk<GetAccountsWithResourcesUseCase>()
    private val incomingRequestRepository = mockk<IncomingRequestRepository>()
    private val checkMnemonicIntegrityUseCase = mockk<CheckMnemonicIntegrityUseCase>()

    private val fromAccount = account(
        address = "account_tdx_19jd32jd3928jd3892jd329",
        name = "From Account"
    )
    private val otherAccounts = listOf(
        account(
            address = "account_tdx_3j892dj3289dj32d2d2d2d9",
            name = "To Account 1"
        ),
        account(
            address = "account_tdx_39jfc32jd932ke9023j89r9",
            name = "To Account 2"
        ),
        account(
            address = "account_tdx_12901829jd9281jd189jd98",
            name = "To account 3"
        )
    )

    override fun initVM(): TransferViewModel {
        return TransferViewModel(
            getProfileUseCase = getProfileUseCase,
            getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase,
            incomingRequestRepository = incomingRequestRepository,
            checkMnemonicIntegrityUseCase = checkMnemonicIntegrityUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @Before
    override fun setUp() = runTest {
        super.setUp()
        every { savedStateHandle.get<String>(ARG_ACCOUNT_ID) } returns fromAccount.address
        every { getProfileUseCase() } returns flowOf(profile(accounts = listOf(fromAccount) + otherAccounts))
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
    fun `choosing an third party address from the accounts chooser`() = runTest {
        val viewModel = vm.value
        viewModel.state.test {
            assertFromAccountSet()
            assertOpenSheetForSkeleton(viewModel, viewModel.state.value.targetAccounts[0] as TargetAccount.Skeleton)
            assertOtherAccountSubmitted(viewModel, "account_tdx_e_12ypd8nyhsej537x3am8nnjzsef45ttmua5tf7f8lz2zds78dgg5qzx")
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
                ownedAccounts = persistentListOf()
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
                    ownedAccounts = remainingAccounts.toPersistentList()
                ),
                awaitItem().sheet
            )
        }
    }

    private suspend fun ReceiveTurbine<TransferViewModel.State>.assertSubmittingOwnedAccount(
        viewModel: TransferViewModel,
        account: Network.Account
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
                address = address,
                validity = TargetAccount.Other.AddressValidity.VALID,
                id = skeletonAccount.id
            )
        )
        // Check that the owned accounts are disabled for selection
        assertFalse(sheetState.isOwnedAccountsEnabled)

        viewModel.onChooseAccountSubmitted()
        assertEquals(
            TransferViewModel.State(
                fromAccount = fromAccount,
                targetAccounts = persistentListOf(
                    TargetAccount.Other(
                        address = address,
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

