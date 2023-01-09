package com.babylon.wallet.android.presentation.createaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.CreateAccountUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
) : ViewModel(), OneOffEventHandler<CreateAccountEvent> by OneOffEventHandlerImpl() {

    private val args = CreateAccountNavArgs(savedStateHandle)
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
            val hasProfile = profileRepository.readProfileSnapshot() != null
            val account = if (hasProfile) {
                createAccountUseCase(
                    displayName = accountName.value,
                    networkUrl = args.networkUrlEncoded?.let {
                        URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                    },
                    networkName = args.networkName,
                    switchNetwork = args.switchNetwork ?: false
                )
            } else {
                val profile = generateProfileUseCase(
                    accountDisplayName = accountName.value,
                )
                profile.perNetwork.first().accounts.first()
            }
            val accountId = account.entityAddress.address
            val accountName = accountName.value

            state = state.copy(
                loading = true,
                accountId = accountId,
                accountName = accountName,
                hasProfile = hasProfile
            )

            if (hasProfile) {
                sendEvent(
                    CreateAccountEvent.Complete(
                        accountId = accountId,
                        args.requestSource
                    )
                )
            } else {
                sendEvent(
                    CreateAccountEvent.NavigateToWallet
                )
            }
        }
    }

    data class CreateAccountState(
        val loading: Boolean = false,
        val accountId: String = "",
        val accountName: String = "",
        val hasProfile: Boolean = false,
    )

    companion object {
        private const val ACCOUNT_NAME_MAX_LENGTH = 20
        private const val ACCOUNT_NAME = "account_name"
        private const val CREATE_ACCOUNT_BUTTON_ENABLED = "create_account_button_enabled"
    }
}

internal sealed interface CreateAccountEvent : OneOffEvent {
    data class Complete(
        val accountId: String,
        val requestSource: CreateAccountRequestSource?,
    ) : CreateAccountEvent
    object NavigateToWallet : CreateAccountEvent
}
