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
import com.babylon.wallet.android.presentation.ui.composables.RenameInput
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
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
    private val appEventBus: AppEventBus,
) : StateViewModel<AccountSettingsViewModel.State>(),
    OneOffEventHandler<AccountSettingsViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = AccountSettingsArgs(savedStateHandle)

    override fun initialState(): State = State()

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
                        renameAccountInput = state.renameAccountInput.copy(name = account.displayName.value),
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
                renameAccountInput = accountPreferenceUiState.renameAccountInput.copy(name = accountNameChanged)
            )
        }
    }

    fun onRenameAccountNameConfirm() {
        viewModelScope.launch {
            val accountToRename = getProfileUseCase().activeAccountsOnCurrentNetwork.find {
                args.address == it.address
            }
            _state.update { state ->
                state.copy(
                    renameAccountInput = state.renameAccountInput.copy(isUpdating = true)
                )
            }
            accountToRename?.let {
                val newAccountName = _state.value.renameAccountInput.name.trim()
                renameAccountDisplayNameUseCase(
                    accountToRename = it,
                    newDisplayName = DisplayName(newAccountName)
                )
                onDismissBottomSheet()
                _state.update { state -> state.copy(isAccountNameUpdated = true) }
            } ?: Timber.d("Couldn't find account to rename the display name!")
        }
    }

    fun onDismissBottomSheet() {
        _state.update {
            it.copy(bottomSheetContent = State.BottomSheetContent.None)
        }
    }

    fun onRenameAccountRequest() {
        _state.update { state ->
            state.copy(
                renameAccountInput = State.RenameAccountInput(name = state.account?.displayName?.value.orEmpty()),
                bottomSheetContent = State.BottomSheetContent.RenameAccount
            )
        }
    }

    fun onHideAccountRequest() {
        _state.update {
            it.copy(bottomSheetContent = State.BottomSheetContent.HideAccount)
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

            onDismissBottomSheet()
            sendEvent(Event.AccountHidden)
        }
    }

    fun onSnackbarMessageShown() {
        _state.update { state -> state.copy(isAccountNameUpdated = false) }
    }

    fun onDeleteAccountRequest() {
        val account = state.value.account ?: return
        viewModelScope.launch {
            sendEvent(Event.OpenDeleteAccount(accountAddress = account.address))
        }
    }

    data class State(
        val settingsSections: ImmutableList<AccountSettingsSection> = defaultSettings,
        val account: Account? = null,
        val renameAccountInput: RenameAccountInput = RenameAccountInput(),
        val bottomSheetContent: BottomSheetContent = BottomSheetContent.None,
        val error: UiMessage? = null,
        val faucetState: FaucetState = FaucetState.Unavailable,
        val isAccountNameUpdated: Boolean = false,
        val isFreeXRDLoading: Boolean = false
    ) : UiState {

        data class RenameAccountInput(
            override val name: String = "",
            override val isUpdating: Boolean = false
        ) : RenameInput()

        val isBottomSheetVisible: Boolean
            get() = bottomSheetContent != BottomSheetContent.None

        sealed interface BottomSheetContent {
            data object None : BottomSheetContent
            data object RenameAccount : BottomSheetContent
            data object HideAccount : BottomSheetContent
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

    sealed interface Event : OneOffEvent {
        data object AccountHidden : Event

        data class OpenDeleteAccount(val accountAddress: AccountAddress) : Event
    }
}
