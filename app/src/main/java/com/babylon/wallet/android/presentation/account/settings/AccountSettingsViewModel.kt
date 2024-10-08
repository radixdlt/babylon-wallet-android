package com.babylon.wallet.android.presentation.account.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.usecases.FaucetState
import com.babylon.wallet.android.domain.usecases.GetFreeXrdUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.Constants.ENTITY_NAME_MAX_LENGTH
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.DisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.ChangeEntityVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.RenameAccountDisplayNameUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class AccountSettingsViewModel @Inject constructor(
    private val getFreeXrdUseCase: GetFreeXrdUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val renameAccountDisplayNameUseCase: RenameAccountDisplayNameUseCase,
    savedStateHandle: SavedStateHandle,
    private val changeEntityVisibilityUseCase: ChangeEntityVisibilityUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
    private val appEventBus: AppEventBus
) : StateViewModel<AccountPreferenceUiState>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = AccountSettingsArgs(savedStateHandle)

    override fun initialState(): AccountPreferenceUiState = AccountPreferenceUiState()

    init {
        loadAccount()
        viewModelScope.launch {
            if (!BuildConfig.DEBUG_MODE) return@launch

            val developerSectionExist = state.value.settingsSections.any {
                it is AccountSettingsSection.DevelopmentSection
            }
            if (developerSectionExist) return@launch
            _state.update {
                it.copy(
                    settingsSections = (
                        it.settingsSections + AccountSettingsSection.DevelopmentSection(
                            listOf(
                                AccountSettingItem.DevSettings
                            )
                        )
                        ).toPersistentList()
                )
            }
        }
        viewModelScope.launch {
            getFreeXrdUseCase.getFaucetState(args.address).collect { faucetState ->
                _state.update { it.copy(faucetState = faucetState) }
            }
        }
    }

    private fun loadAccount() {
        viewModelScope.launch {
            getProfileUseCase.flow.mapNotNull { profile ->
                profile.activeAccountsOnCurrentNetwork.firstOrNull { it.address == args.address }
            }.collect { account ->
                val thirdPartyDefaultDepositRule = account.onLedgerSettings.thirdPartyDeposits.depositRule
                _state.update { state ->
                    state.copy(
                        accountNameChanged = account.displayName.value,
                        account = account,
                        settingsSections = state.settingsSections.mapWhen(
                            predicate = { it is AccountSettingsSection.AccountSection },
                            mutation = { section ->
                                val updatedSection = section as AccountSettingsSection.AccountSection
                                updatedSection.copy(
                                    settingsItems = updatedSection.settingsItems.mapWhen(
                                        predicate = { it is AccountSettingItem.ThirdPartyDeposits },
                                        mutation = {
                                            AccountSettingItem.ThirdPartyDeposits(thirdPartyDefaultDepositRule)
                                        }
                                    )
                                )
                            }
                        ).toPersistentList()
                    )
                }
            }
        }
    }

    fun onRenameAccountNameChange(accountNameChanged: String) {
        _state.update { accountPreferenceUiState ->
            accountPreferenceUiState.copy(
                accountNameChanged = accountNameChanged,
                isNewNameValid = accountNameChanged.isNotBlank() && accountNameChanged.count() <= ENTITY_NAME_MAX_LENGTH,
                isNewNameLengthMoreThanTheMaximum = accountNameChanged.count() > ENTITY_NAME_MAX_LENGTH
            )
        }
    }

    fun onRenameAccountNameConfirm() {
        viewModelScope.launch {
            val accountToRename = getProfileUseCase().activeAccountsOnCurrentNetwork.find {
                args.address == it.address
            }
            accountToRename?.let {
                val newAccountName = _state.value.accountNameChanged.trim()
                renameAccountDisplayNameUseCase(
                    accountToRename = it,
                    newDisplayName = DisplayName(newAccountName)
                )
                _state.update { state -> state.copy(isAccountNameUpdated = true) }
            } ?: Timber.d("Couldn't find account to rename the display name!")
        }
    }

    fun setBottomSheetContent(content: AccountPreferenceUiState.BottomSheetContent) {
        _state.update {
            it.copy(bottomSheetContent = content)
        }
    }

    fun onGetFreeXrdClick() {
        if (state.value.faucetState !is FaucetState.Available) return

        appScope.launch {
            _state.update { it.copy(isFreeXRDLoading = true) }
            getFreeXrdUseCase(address = args.address).onSuccess { _ ->
                _state.update { it.copy(isFreeXRDLoading = false) }
                appEventBus.sendEvent(AppEvent.RefreshAssetsNeeded)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isFreeXRDLoading = false,
                        error = UiMessage.ErrorMessage(error = error)
                    )
                }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    fun onHideAccount() {
        viewModelScope.launch {
            val account = state.value.account ?: return@launch
            changeEntityVisibilityUseCase.changeAccountVisibility(entityAddress = account.address, hide = true)
            setBottomSheetContent(AccountPreferenceUiState.BottomSheetContent.None)
            sendEvent(Event.AccountHidden)
        }
    }

    fun onSnackbarMessageShown() {
        _state.update { state -> state.copy(isAccountNameUpdated = false) }
    }
}

sealed interface Event : OneOffEvent {
    data object AccountHidden : Event
}

data class AccountPreferenceUiState(
    val settingsSections: ImmutableList<AccountSettingsSection> = defaultSettings,
    val account: Account? = null,
    val accountNameChanged: String = "",
    val isNewNameValid: Boolean = false,
    val isNewNameLengthMoreThanTheMaximum: Boolean = false,
    val bottomSheetContent: BottomSheetContent = BottomSheetContent.None,
    val error: UiMessage? = null,
    val faucetState: FaucetState = FaucetState.Unavailable,
    val isAccountNameUpdated: Boolean = false,
    val isFreeXRDLoading: Boolean = false
) : UiState {

    val isBottomSheetVisible: Boolean
        get() = bottomSheetContent != BottomSheetContent.None

    enum class BottomSheetContent {
        None, RenameAccount, HideAccount
    }

    companion object {
        val defaultSettings = persistentListOf(
            AccountSettingsSection.PersonalizeSection(listOf(AccountSettingItem.AccountLabel)),
            AccountSettingsSection.AccountSection(
                listOf(AccountSettingItem.ThirdPartyDeposits(DepositRule.ACCEPT_ALL))
            )
        )
    }
}
