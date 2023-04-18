package com.babylon.wallet.android.presentation.createaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import com.babylon.wallet.android.utils.decodeUtf8
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetProfileStateUseCase
import rdx.works.profile.domain.account.CreateAccountUseCase
import rdx.works.profile.domain.exists
import javax.inject.Inject

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getProfileStateUseCase: GetProfileStateUseCase,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val deviceSecurityHelper: DeviceSecurityHelper,
) : StateViewModel<CreateAccountViewModel.CreateAccountState>(),
    OneOffEventHandler<CreateAccountEvent> by OneOffEventHandlerImpl() {

    private val args = CreateAccountNavArgs(savedStateHandle)
    val accountName = savedStateHandle.getStateFlow(ACCOUNT_NAME, "")
    val buttonEnabled = savedStateHandle.getStateFlow(CREATE_ACCOUNT_BUTTON_ENABLED, false)

    override fun initialState(): CreateAccountState = CreateAccountState(
        isDeviceSecure = deviceSecurityHelper.isDeviceSecure(),
        firstTime = args.requestSource == CreateAccountRequestSource.FirstTime
    )

    fun onAccountNameChange(accountName: String) {
        savedStateHandle[ACCOUNT_NAME] = accountName.take(ACCOUNT_NAME_MAX_LENGTH)
        savedStateHandle[CREATE_ACCOUNT_BUTTON_ENABLED] = accountName.trim().isNotEmpty()
    }

    fun onAccountCreateClick() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val hasProfile = getProfileStateUseCase.exists()
            val accountName = accountName.value.trim()
            val account = if (hasProfile) {
                createAccountUseCase(
                    displayName = accountName,
                    networkUrl = args.networkUrlEncoded?.decodeUtf8(),
                    networkName = args.networkName,
                    switchNetwork = args.switchNetwork ?: false
                )
            } else {
                val profile = generateProfileUseCase(
                    accountDisplayName = accountName,
                )
                profile.networks.first().accounts.first()
            }
            val accountId = account.address

            _state.update {
                it.copy(
                    loading = true,
                    accountId = accountId,
                    accountName = accountName,
                    hasProfile = hasProfile
                )
            }

            sendEvent(
                CreateAccountEvent.Complete(
                    accountId = accountId,
                    args.requestSource
                )
            )
        }
    }

    data class CreateAccountState(
        val loading: Boolean = false,
        val accountId: String = "",
        val accountName: String = "",
        val hasProfile: Boolean = false,
        val isDeviceSecure: Boolean = false,
        val firstTime: Boolean = false,
    ) : UiState

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
}
