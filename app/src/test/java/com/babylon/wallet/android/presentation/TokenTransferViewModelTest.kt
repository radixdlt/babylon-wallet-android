package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.mockdata.account1
import com.babylon.wallet.android.mockdata.account2
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transfer.TokenTransferUseCase
import com.babylon.wallet.android.presentation.transfer.TokenTransferViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.repository.AccountRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TokenTransferViewModelTest : BaseViewModelTest<TokenTransferViewModel>() {

    private val tokenTransferUseCase = mockk<TokenTransferUseCase>()
    private val accountRepository = mockk<AccountRepository>()

    private val accounts = listOf(account1, account2)

    @Before
    override fun setUp() = runTest {
        super.setUp()
        coEvery { accountRepository.getAccounts() } returns accounts
    }

    @Test
    fun `when view init, verify all accounts are fetched`() = runTest {
        // when
        val viewModel = vm.value
        advanceUntilIdle()

        // then
        assert(viewModel.state.accounts == accounts)
    }

    @Test
    fun `when sender address is provided only, verify button disabled`() = runTest {
        // given
        val senderAddress = "account_tdx_a_1230r302r32"
        val viewModel = vm.value
        advanceUntilIdle()

        // when
        viewModel.onSenderAddressChanged(senderAddress)

        // then
        assert(viewModel.state.senderAddress == senderAddress)
        assert(!viewModel.state.buttonEnabled)
    }

    @Test
    fun `when sender and recipient address is provided only, verify button disabled`() = runTest {
        // given
        val senderAddress = "account_tdx_a_1230r302r32"
        val recipientAddress = "account_tdx_a_0kd21kd9032k"
        val viewModel = vm.value
        advanceUntilIdle()

        // when
        viewModel.onSenderAddressChanged(senderAddress)
        viewModel.onRecipientAddressChanged(recipientAddress)

        // then
        assert(viewModel.state.senderAddress == senderAddress)
        assert(viewModel.state.recipientAddress == recipientAddress)
        assert(!viewModel.state.buttonEnabled)
    }

    @Test
    fun `when sender and recipient address and amount is provided, verify button enabled`() = runTest {
        // given
        val senderAddress = "account_tdx_a_1230r302r32"
        val recipientAddress = "account_tdx_a_0kd21kd9032k"
        val tokenAmount = "100"
        val viewModel = vm.value
        advanceUntilIdle()

        // when
        viewModel.onSenderAddressChanged(senderAddress)
        viewModel.onRecipientAddressChanged(recipientAddress)
        viewModel.onTokenAmountChanged(tokenAmount)

        // then
        assert(viewModel.state.senderAddress == senderAddress)
        assert(viewModel.state.recipientAddress == recipientAddress)
        assert(viewModel.state.tokenAmount == tokenAmount)
        assert(viewModel.state.buttonEnabled)
    }

    @Test
    fun `given all fields have values, when transfer succeeds upon click, verify transfer complete callback hit`() =
        runTest {
            // given
            val senderAddress = "account_tdx_a_1230r302r32"
            val recipientAddress = "account_tdx_a_0kd21kd9032k"
            val tokenAmount = "100"
            val viewModel = vm.value
            coEvery {
                tokenTransferUseCase.invoke(senderAddress, recipientAddress, tokenAmount)
            } returns Result.Success("success")

            viewModel.onSenderAddressChanged(senderAddress)
            viewModel.onRecipientAddressChanged(recipientAddress)
            viewModel.onTokenAmountChanged(tokenAmount)

            // when
            viewModel.onTransferClick()
            advanceUntilIdle()

            // then
            assert(viewModel.state.transferComplete)
        }

    @Test
    fun `given all fields have values, when transfer fails upon click, verify transfer complete callback not hit`() =
        runTest {
            // given
            val senderAddress = "account_tdx_a_1230r302r32"
            val recipientAddress = "account_tdx_a_0kd21kd9032k"
            val tokenAmount = "100"
            val exception = Throwable("transfer failed")
            val viewModel = vm.value
            coEvery {
                tokenTransferUseCase.invoke(senderAddress, recipientAddress, tokenAmount)
            } returns Result.Error(exception)

            viewModel.onSenderAddressChanged(senderAddress)
            viewModel.onRecipientAddressChanged(recipientAddress)
            viewModel.onTokenAmountChanged(tokenAmount)

            // when
            viewModel.onTransferClick()
            advanceUntilIdle()

            // then
            assert(!viewModel.state.transferComplete)
            assert(viewModel.state.error == UiMessage.ErrorMessage(error = exception))
        }

    override fun initVM(): TokenTransferViewModel {
        return TokenTransferViewModel(tokenTransferUseCase, accountRepository)
    }
}