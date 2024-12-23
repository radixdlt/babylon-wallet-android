package com.babylon.wallet.android.presentation.account.settings.devsettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class DevSettingsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<DevSettingsUiState>() {

    private val args = DevSettingsArgs(savedStateHandle)

    override fun initialState(): DevSettingsUiState = DevSettingsUiState(
        accountAddress = args.address,
    )

    init {
        loadAccount()
    }

    private fun loadAccount() {
        viewModelScope.launch {
            getProfileUseCase.flow.mapNotNull { profile ->
                profile.activeAccountsOnCurrentNetwork.firstOrNull { it.address == args.address }
            }.collect { account ->
                _state.update { state ->
                    state.copy(
                        account = account
                    )
                }
            }
        }
    }
}

data class DevSettingsUiState(
    val account: Account? = null,
    val accountAddress: AccountAddress,
    val isLoading: Boolean = false,
) : UiState
