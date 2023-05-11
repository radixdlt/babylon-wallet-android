package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import rdx.works.profile.domain.GetProfileUseCase

@ExperimentalCoroutinesApi
class TransferViewModelTest : StateViewModelTest<TransferViewModel>() {

    private val savedStateHandle = Mockito.mock(SavedStateHandle::class.java)
    private val getProfileUseCase = Mockito.mock(GetProfileUseCase::class.java)

    private val accounts = listOf(
        account(
            address = "account_tdx_19jd32jd3928jd3892jd329",
            name = "Account1"
        ),
        account(
            address = "account_tdx_3j892dj3289dj32d2d2d2d9",
            name = "Account2"
        ),
        account(
            address = "account_tdx_39jfc32jd932ke9023j89r9",
            name = "Account3"
        ),
        account(
            address = "account_tdx_12901829jd9281jd189jd98",
            name = "Account4"
        )
    )

    override fun initVM(): TransferViewModel {
       return TransferViewModel(
           getProfileUseCase = getProfileUseCase,
           savedStateHandle = savedStateHandle
       )
    }

    @Before
    override fun setUp() = runTest {
        super.setUp()
        whenever(savedStateHandle.get<String>(ARG_ACCOUNT_ID)).thenReturn(accounts.first().address)
        whenever(getProfileUseCase()).thenReturn(flowOf(profile(accounts = accounts)))
    }

    @Test
    fun `when viewmodel init, verify correcy sender acc is displayed and destination accounts are shown`() = runTest {
        val firstAccount = accounts[0]
        val secondAccount = accounts[1]
        val thirdAccount = accounts[2]
        val fourthAccount = accounts[3]

        val viewModel = vm.value
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            AccountItemUiModel(
                address = firstAccount.address,
                displayName = firstAccount.displayName,
                appearanceID = firstAccount.appearanceID,
                isSelected = false
            ),
            viewModel.state.first().fromAccount
        )
        Assert.assertEquals(
            persistentListOf(
                AccountItemUiModel(
                    address = secondAccount.address,
                    displayName = secondAccount.displayName,
                    appearanceID = secondAccount.appearanceID,
                    isSelected = false
                ),
                AccountItemUiModel(
                    address = thirdAccount.address,
                    displayName = thirdAccount.displayName,
                    appearanceID = thirdAccount.appearanceID,
                    isSelected = false
                ),
                AccountItemUiModel(
                    address = fourthAccount.address,
                    displayName = fourthAccount.displayName,
                    appearanceID = fourthAccount.appearanceID,
                    isSelected = false
                )
            ),
            viewModel.state.first().receivingAccounts
        )
    }

    @Test
    fun `when message changed verify it is reflected in the ui state`() = runTest {
        val message = "New Message"
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.onMessageChanged(message)

        Assert.assertEquals(
            viewModel.state.first().message,
            message
        )
    }

    @Test
    fun `when chose account clicked with index verify it is reflected in the ui state with correct index`() = runTest {
        val selectedIndex = 1
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.onChooseClick(selectedIndex)

        Assert.assertEquals(
            viewModel.state.first().chosenAccountIndex,
            selectedIndex
        )
    }

    @Test
    fun `when qrcode icon clicked, verify view mode is changed to ScanQR`() = runTest {
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.onQrCodeIconClick()

        Assert.assertEquals(
            viewModel.state.first().chooseAccountSheetMode,
            ChooseAccountSheetMode.ScanQr
        )
    }

    @Test
    fun `when cancel qrscan invoked, verify view mode is changed to Default`() = runTest {
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.cancelQrScan()

        Assert.assertEquals(
            viewModel.state.first().chooseAccountSheetMode,
            ChooseAccountSheetMode.Default
        )
    }

    @Test
    fun `when valid address decoded from qr scanner, verify view mode is back to Default and choose button enabled`() =
        runTest {
            val address = "account_rdx_1923ej83292"
            val viewModel = vm.value
            advanceUntilIdle()

            viewModel.onAddressDecoded(address)

            Assert.assertEquals(
                viewModel.state.first().address,
                address
            )
            Assert.assertEquals(
                viewModel.state.first().chooseAccountSheetMode,
                ChooseAccountSheetMode.Default
            )
            Assert.assertEquals(
                viewModel.state.first().buttonEnabled,
                true
            )
        }

    @Test
    fun `when empty address decoded from qr scanner, verify view mode is back to Default and choose button disabled`() =
        runTest {
            val address = ""
            val viewModel = vm.value
            advanceUntilIdle()

            viewModel.onAddressDecoded(address)

            Assert.assertEquals(
                viewModel.state.first().address,
                address
            )
            Assert.assertEquals(
                viewModel.state.first().chooseAccountSheetMode,
                ChooseAccountSheetMode.Default
            )
            Assert.assertEquals(
                viewModel.state.first().buttonEnabled,
                false
            )
        }

    @Test
    fun `when valid address entered in the field, verify all accounts are disabled and unselected`() = runTest {
        val address = "account_rdx_1923ej83292"
        val receivingAccounts = accounts.toMutableList()
        receivingAccounts.removeFirst()
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.onAddressChanged(address)

        Assert.assertEquals(
            viewModel.state.first().address,
            address
        )
        Assert.assertEquals(
            viewModel.state.first().receivingAccounts,
            receivingAccounts.map {
                AccountItemUiModel(
                    address = it.address,
                    displayName = it.displayName,
                    appearanceID = it.appearanceID,
                    isSelected = false
                )
            }
        )
        Assert.assertEquals(
            viewModel.state.first().accountsDisabled,
            true
        )
    }

    @Test
    fun `when new empty account added, verify two accounts are shown`() = runTest {
        val expectedAccounts = persistentListOf(
            TransferViewModel.State.SelectedAccountForTransfer(
                account = null,
                type = TransferViewModel.State.SelectedAccountForTransfer.Type.NoAccount
            ),
            TransferViewModel.State.SelectedAccountForTransfer(
                account = null,
                type = TransferViewModel.State.SelectedAccountForTransfer.Type.NoAccount
            )
        )
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.addAccountClick()

        Assert.assertEquals(
            viewModel.state.first().selectedAccounts,
            expectedAccounts
        )
    }

    @Test
    fun `when empty account added and removed, verify just one account is shown`() = runTest {
        val expectedAccounts = persistentListOf(
            TransferViewModel.State.SelectedAccountForTransfer(
                account = null,
                type = TransferViewModel.State.SelectedAccountForTransfer.Type.NoAccount
            )
        )
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.addAccountClick()
        advanceUntilIdle()
        viewModel.deleteAccountClick(1)

        Assert.assertEquals(
            viewModel.state.first().selectedAccounts,
            expectedAccounts
        )
    }

    @Test
    fun `when empty account added twice and removed just one, verify just two accounts are shown`() = runTest {
        val expectedAccounts = persistentListOf(
            TransferViewModel.State.SelectedAccountForTransfer(
                account = null,
                type = TransferViewModel.State.SelectedAccountForTransfer.Type.NoAccount
            ),
            TransferViewModel.State.SelectedAccountForTransfer(
                account = null,
                type = TransferViewModel.State.SelectedAccountForTransfer.Type.NoAccount
            )
        )
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.addAccountClick()
        advanceUntilIdle()
        viewModel.addAccountClick()
        advanceUntilIdle()
        viewModel.deleteAccountClick(1)

        Assert.assertEquals(
            viewModel.state.first().selectedAccounts,
            expectedAccounts
        )
    }

    @Test
    fun `when selected first account, verify it is selected in ui state`() = runTest {
        val destinationAccounts = accounts.toMutableList()
        destinationAccounts.removeFirst()
        val receivingAccounts = destinationAccounts.mapIndexed { index, account ->
            AccountItemUiModel(
                address = account.address,
                displayName = account.displayName,
                appearanceID = account.appearanceID,
                isSelected = index == 0
            )
        }.toPersistentList()
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.onAccountSelect(0)

        Assert.assertEquals(
            viewModel.state.first().buttonEnabled,
            true
        )
        Assert.assertEquals(
            viewModel.state.first().accountsDisabled,
            false
        )
        Assert.assertEquals(
            viewModel.state.first().receivingAccounts,
            receivingAccounts
        )
    }

    @Test
    fun `when selected first account, verify it is shown in selected accounts`() = runTest {
        val selectedIndex = 0
        val destinationAccounts = accounts.toMutableList()
        destinationAccounts.removeFirst()
        val account = destinationAccounts[selectedIndex]
        val selectedAccount = TransferViewModel.State.SelectedAccountForTransfer(
            account = AccountItemUiModel(
                address = account.address,
                displayName = account.displayName,
                appearanceID = account.appearanceID,
                isSelected = true
            ),
            type = TransferViewModel.State.SelectedAccountForTransfer.Type.ExistingAccount
        )
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.onChooseClick(0)
        advanceUntilIdle()
        viewModel.onAccountSelect(selectedIndex)
        advanceUntilIdle()
        viewModel.onChooseDestinationAccountClick()
        advanceUntilIdle()

        Assert.assertEquals(
            viewModel.state.first().selectedAccounts,
            persistentListOf(selectedAccount)
        )
    }

    @Test
    fun `when selected second account, verify it is shown in selected accounts`() = runTest {
        val selectedIndex = 1
        val destinationAccounts = accounts.toMutableList()
        destinationAccounts.removeFirst()
        val account = destinationAccounts[selectedIndex]
        val selectedAccount = TransferViewModel.State.SelectedAccountForTransfer(
            account = AccountItemUiModel(
                address = account.address,
                displayName = account.displayName,
                appearanceID = account.appearanceID,
                isSelected = true
            ),
            type = TransferViewModel.State.SelectedAccountForTransfer.Type.ExistingAccount
        )
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.onChooseClick(0)
        advanceUntilIdle()
        viewModel.onAccountSelect(selectedIndex)
        advanceUntilIdle()
        viewModel.onChooseDestinationAccountClick()
        advanceUntilIdle()

        Assert.assertEquals(
            viewModel.state.first().selectedAccounts,
            persistentListOf(selectedAccount)
        )
    }

    @Test
    fun `when scanned third party account and confirmed, verify it is shown in selected accounts`() = runTest {
        val thirdPartyAccountsAddress = "account_tdx_19k2019dk20"
        val selectedAccount = TransferViewModel.State.SelectedAccountForTransfer(
            account = AccountItemUiModel(
                address = thirdPartyAccountsAddress,
                displayName = "Account",
                appearanceID = 0,
                isSelected = false
            ),
            type = TransferViewModel.State.SelectedAccountForTransfer.Type.ThirdPartyAccount
        )
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.onChooseClick(0)
        advanceUntilIdle()
        viewModel.onAddressDecoded(thirdPartyAccountsAddress)
        advanceUntilIdle()
        viewModel.onChooseDestinationAccountClick()
        advanceUntilIdle()

        Assert.assertEquals(
            viewModel.state.first().selectedAccounts,
            persistentListOf(selectedAccount)
        )
    }

    @Test
    fun `when entered third party account and confirmed, verify it is shown in selected accounts`() = runTest {
        val thirdPartyAccountsAddress = "account_tdx_19k2019dk20"
        val selectedAccount = TransferViewModel.State.SelectedAccountForTransfer(
            account = AccountItemUiModel(
                address = thirdPartyAccountsAddress,
                displayName = "Account",
                appearanceID = 0,
                isSelected = false
            ),
            type = TransferViewModel.State.SelectedAccountForTransfer.Type.ThirdPartyAccount
        )
        val viewModel = vm.value
        advanceUntilIdle()

        viewModel.onChooseClick(0)
        advanceUntilIdle()
        viewModel.onAddressChanged(thirdPartyAccountsAddress)
        advanceUntilIdle()
        viewModel.onChooseDestinationAccountClick()
        advanceUntilIdle()

        Assert.assertEquals(
            viewModel.state.first().selectedAccounts,
            persistentListOf(selectedAccount)
        )
    }
}
