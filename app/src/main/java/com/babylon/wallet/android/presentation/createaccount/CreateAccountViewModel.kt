package com.babylon.wallet.android.presentation.createaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.CreateAccountUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import javax.inject.Inject

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val createAccountUseCase: CreateAccountUseCase
) : ViewModel() {

    val accountName = savedStateHandle.getStateFlow(ACCOUNT_NAME, "")

    val buttonEnabled = savedStateHandle.getStateFlow(CREATE_ACCOUNT_BUTTON_ENABLED, false)

    var state by mutableStateOf(CreateAccountState())
        private set

    fun onAccountNameChange(accountName: String) {
        savedStateHandle[ACCOUNT_NAME] = accountName.take(ACCOUNT_NAME_MAX_LENGTH)
        savedStateHandle[CREATE_ACCOUNT_BUTTON_ENABLED] = accountName.isNotEmpty()
    }

    fun onAccountCreateClick() {
        state = state.copy(
            loading = true
        )
        viewModelScope.launch {

            val profileExists = profileRepository.readProfileSnapshot() != null

            val account = if (profileExists) {
                createAccountUseCase(
                    displayName = accountName.value
                )
            } else {
                val profile = generateProfileUseCase(
                    accountDisplayName = accountName.value
                )
                profile.perNetwork.first().accounts.first()
            }

            state = state.copy(
                loading = true,
                complete = true,
                accountId = account.address.address,
                accountName = accountName.value,
                profileExists = profileExists
            )
        }
    }

    data class CreateAccountState(
        val loading: Boolean = false,
        val complete: Boolean = false,
        val accountId: String = "",
        val accountName: String = "",
        val profileExists: Boolean = false
    )

    companion object {
        private const val ACCOUNT_NAME_MAX_LENGTH = 20
        private const val ACCOUNT_NAME = "account_name"
        private const val CREATE_ACCOUNT_BUTTON_ENABLED = "create_account_button_enabled"
    }
}
