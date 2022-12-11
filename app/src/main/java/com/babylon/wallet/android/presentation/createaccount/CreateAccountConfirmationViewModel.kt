package com.babylon.wallet.android.presentation.createaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.babylon.wallet.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateAccountConfirmationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val profileExists = savedStateHandle.get<Boolean>(Screen.ARG_PROFILE_EXISTS) ?: false
    private val accountName = savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME).orEmpty()
    private val accountId = savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID).orEmpty()

    var accountUiState by mutableStateOf(
        AccountConfirmationUiState(
            accountName = accountName,
            accountId = accountId
        )
    )
        private set

    fun goHomeClick() {
        accountUiState = if (profileExists) {
            accountUiState.copy(
                dismiss = true,
                goNext = false,
                accountName = accountName,
                accountId = accountId
            )
        } else {
            accountUiState.copy(
                dismiss = false,
                goNext = true,
                accountName = accountName,
                accountId = accountId
            )
        }
    }

    data class AccountConfirmationUiState(
        val dismiss: Boolean = false,
        val goNext: Boolean = false,
        val accountName: String = "",
        val accountId: String = ""
    )
}
