package com.babylon.wallet.android.presentation.settings.account.thirdpartydeposits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.account.AccountSettingItem
import com.babylon.wallet.android.presentation.settings.account.AccountSettingsSection
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class AccountThirdPartyDepositsViewModel @Inject constructor(
    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<AccountThirdPartyDepositsUiState>() {

    private val args = AccountThirdPartyDepositsArgs(savedStateHandle)

    override fun initialState(): AccountThirdPartyDepositsUiState = AccountThirdPartyDepositsUiState(accountAddress = args.address)

    init {
        loadAccount()
    }

    fun onAllowAll() {
        // TODO not implemented
    }

    fun onDenyAll() {
        // TODO not implemented
    }

    fun onAcceptKnown() {
        // TODO not implemented
    }

    private fun loadAccount() {
        viewModelScope.launch {
            getProfileUseCase.accountsOnCurrentNetwork.map { accounts -> accounts.first { it.address == args.address } }
                .collect { account ->
                    _state.update {
                        it.copy(
                            account = account,
                            isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
                        )
                    }
                }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }
}

data class AccountThirdPartyDepositsUiState(
    val settingsSections: ImmutableList<AccountSettingsSection> = defaultSettings,
    val account: Network.Account? = null,
    val accountAddress: String,
    val canUseFaucet: Boolean = false,
    val isLoading: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val error: UiMessage? = null,
) : UiState {
    companion object {
        val defaultSettings = persistentListOf(AccountSettingsSection.AccountSection(listOf(AccountSettingItem.ThirdPartyDeposits)))
    }
}
