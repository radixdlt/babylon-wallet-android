package com.babylon.wallet.android.presentation.transfer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class TokenTransferViewModel @Inject constructor(
    private val tokenTransferUseCase: TokenTransferUseCase,
    private val accountRepository: AccountRepository
) : ViewModel() {

    internal var state by mutableStateOf(TokenTransferUiState())
        private set

    init {
        viewModelScope.launch {
            state = state.copy(
                accounts = accountRepository.getAccounts().toImmutableList()
            )
        }
    }

    fun onSenderAddressChanged(senderAddress: String) {
        val senderAddressTrimmed = senderAddress.trim()
        val fieldsNotEmpty = senderAddressTrimmed.isNotEmpty() &&
            state.recipientAddress.isNotEmpty() &&
            state.tokenAmount.isNotEmpty()
        state = state.copy(
            senderAddress = senderAddressTrimmed,
            buttonEnabled = fieldsNotEmpty
        )
    }

    fun onRecipientAddressChanged(recipientAddress: String) {
        val recipientAddressTrimmed = recipientAddress.trim()
        val fieldsNotEmpty = recipientAddressTrimmed.isNotEmpty() &&
            state.senderAddress.isNotEmpty() &&
            state.tokenAmount.isNotEmpty()
        state = state.copy(
            recipientAddress = recipientAddressTrimmed,
            buttonEnabled = fieldsNotEmpty
        )
    }

    fun onTokenAmountChanged(tokenAmount: String) {
        val tokenAmountTrimmed = tokenAmount.trim()
        val fieldsNotEmpty = tokenAmountTrimmed.isNotEmpty() &&
            state.senderAddress.isNotEmpty() &&
            state.recipientAddress.isNotEmpty()
        state = state.copy(
            tokenAmount = tokenAmountTrimmed,
            buttonEnabled = fieldsNotEmpty
        )
    }

    fun onTransferClick() {
        viewModelScope.launch {
            state = state.copy(
                isLoading = true,
                buttonEnabled = false
            )
            val result = tokenTransferUseCase(
                senderAddress = state.senderAddress,
                recipientAddress = state.recipientAddress,
                tokenAmount = state.tokenAmount
            )
            result.onValue {
                state = state.copy(
                    isLoading = false,
                    transferComplete = true,
                    buttonEnabled = true
                )
            }
            result.onError {
                state = state.copy(
                    isLoading = false,
                    buttonEnabled = true,
                    transferComplete = false,
                    error = UiMessage.ErrorMessage(error = it)
                )
            }
        }
    }

    fun onMessageShown() {
        state = state.copy(error = null)
    }
}

internal data class TokenTransferUiState(
    val accounts: ImmutableList<Account> = persistentListOf(),
    val senderAddress: String = "",
    val recipientAddress: String = "",
    val tokenAmount: String = "",
    val buttonEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val transferComplete: Boolean = false,
    val error: UiMessage? = null,
)
