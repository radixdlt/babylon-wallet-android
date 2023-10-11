package com.babylon.wallet.android.presentation.account.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.domain.usecases.FaucetState
import com.babylon.wallet.android.domain.usecases.GetFreeXrdUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants.ACCOUNT_NAME_MAX_LENGTH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.ChangeEntityVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.RenameAccountDisplayNameUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val getFreeXrdUseCase: GetFreeXrdUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val renameAccountDisplayNameUseCase: RenameAccountDisplayNameUseCase,
    savedStateHandle: SavedStateHandle,
    private val changeEntityVisibilityUseCase: ChangeEntityVisibilityUseCase
) : StateViewModel<AccountPreferenceUiState>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = AccountSettingsArgs(savedStateHandle)

    override fun initialState(): AccountPreferenceUiState = AccountPreferenceUiState(
        accountAddress = args.address,
    )

    init {
        loadAccount()
        viewModelScope.launch {
            getFreeXrdUseCase.getFaucetState(args.address).collect { faucetState ->
                if (shouldShowDeveloperSettings(faucetState)) {
                    _state.update {
                        it.copy(
                            settingsSections = (
                                it.settingsSections + AccountSettingsSection.DevelopmentSection(
                                    listOf(
                                        AccountSettingItem.DevSettings
                                    )
                                )
                                ).distinct().toPersistentList()
                        )
                    }
                }
            }
        }
    }

    private fun shouldShowDeveloperSettings(faucetState: FaucetState) =
        faucetState is FaucetState.Available || BuildConfig.DEBUG_MODE || BuildConfig.EXPERIMENTAL_FEATURES_ENABLED

    private fun loadAccount() {
        viewModelScope.launch {
            getProfileUseCase.accountsOnCurrentNetwork.mapNotNull { accounts ->
                accounts.firstOrNull { it.address == args.address }
            }.collect { account ->
                _state.update { state ->
                    state.copy(
                        accountName = account.displayName,
                        accountNameChanged = account.displayName,
                        account = account
                    )
                }
            }
        }
    }

    fun onRenameAccountNameChange(accountNameChanged: String) {
        _state.update { accountPreferenceUiState ->
            accountPreferenceUiState.copy(
                accountNameChanged = accountNameChanged,
                isNewNameValid = accountNameChanged.isNotBlank() && accountNameChanged.count() <= ACCOUNT_NAME_MAX_LENGTH,
                isNewNameLengthMoreThanTheMaximum = accountNameChanged.count() > ACCOUNT_NAME_MAX_LENGTH
            )
        }
    }

    fun onRenameAccountNameConfirm() {
        viewModelScope.launch {
            val accountToRename = getProfileUseCase.accountsOnCurrentNetwork.first().find {
                args.address == it.address
            }
            accountToRename?.let {
                val newAccountName = _state.value.accountNameChanged.trim()
                renameAccountDisplayNameUseCase(
                    accountToRename = it,
                    newDisplayName = newAccountName
                )
                _state.update { accountPreferenceUiState ->
                    accountPreferenceUiState.copy(accountName = newAccountName)
                }
            } ?: Timber.d("Couldn't find account to rename the display name!")
        }
    }

    fun setBottomSheetContentToRenameAccount() {
        _state.update {
            it.copy(bottomSheetContent = AccountPreferenceUiState.BottomSheetContent.RenameAccount)
        }
    }

    fun setBottomSheetContentToAddressQRCode() {
        _state.update {
            it.copy(bottomSheetContent = AccountPreferenceUiState.BottomSheetContent.AddressQRCode)
        }
    }

    fun resetBottomSheetContent() {
        _state.update {
            it.copy(bottomSheetContent = AccountPreferenceUiState.BottomSheetContent.None)
        }
    }

    fun onHideAccount() {
        viewModelScope.launch {
            changeEntityVisibilityUseCase.hideAccount(state.value.accountAddress)
            sendEvent(Event.AccountHidden)
        }
    }
}

sealed interface Event : OneOffEvent {
    data object AccountHidden : Event
}

data class AccountPreferenceUiState(
    val settingsSections: ImmutableList<AccountSettingsSection> = defaultSettings,
    val account: Network.Account? = null,
    val accountAddress: String,
    val accountName: String = "",
    val accountNameChanged: String = "",
    val isNewNameValid: Boolean = false,
    val isNewNameLengthMoreThanTheMaximum: Boolean = false,
    val bottomSheetContent: BottomSheetContent = BottomSheetContent.None
) : UiState {

    enum class BottomSheetContent {
        None, RenameAccount, AddressQRCode
    }

    companion object {
        val defaultSettings = persistentListOf(
            AccountSettingsSection.PersonalizeSection(listOf(AccountSettingItem.AccountLabel)),
            AccountSettingsSection.AccountSection(listOf(AccountSettingItem.ThirdPartyDeposits))
        )
    }
}
