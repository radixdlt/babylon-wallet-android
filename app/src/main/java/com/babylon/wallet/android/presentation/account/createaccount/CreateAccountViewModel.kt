package com.babylon.wallet.android.presentation.account.createaccount

import Constants.ACCOUNT_NAME_MAX_LENGTH
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.CreateAccountUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.DeleteNotInitializedProfileDataUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.account.SwitchNetworkUseCase
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.ChangeBackupSettingUseCase
import rdx.works.profile.domain.backup.DiscardTemporaryRestoredFileForBackupUseCase
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class CreateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val createAccountUseCase: CreateAccountUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val deleteNotInitializedProfileDataUseCase: DeleteNotInitializedProfileDataUseCase,
    private val discardTemporaryRestoredFileForBackupUseCase: DiscardTemporaryRestoredFileForBackupUseCase,
    private val preferencesManager: PreferencesManager,
    private val switchNetworkUseCase: SwitchNetworkUseCase,
    private val changeBackupSettingUseCase: ChangeBackupSettingUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<CreateAccountViewModel.CreateAccountUiState>(),
    OneOffEventHandler<CreateAccountEvent> by OneOffEventHandlerImpl() {

    private val args = CreateAccountNavArgs(savedStateHandle)
    val accountName = savedStateHandle.getStateFlow(ACCOUNT_NAME, "")
    val buttonEnabled = savedStateHandle.getStateFlow(CREATE_ACCOUNT_BUTTON_ENABLED, false)
    val isAccountNameLengthMoreThanTheMax = savedStateHandle.getStateFlow(IS_ACCOUNT_NAME_LENGTH_MORE_THAN_THE_MAX, false)

    private var accessFactorSourcesJob: Job? = null

    override fun initialState(): CreateAccountUiState = CreateAccountUiState(
        isFirstAccount = args.requestSource?.isFirstTime() == true
    )

    fun onAccountNameChange(accountName: String) {
        savedStateHandle[ACCOUNT_NAME] = accountName
        savedStateHandle[IS_ACCOUNT_NAME_LENGTH_MORE_THAN_THE_MAX] = accountName.count() > ACCOUNT_NAME_MAX_LENGTH
        savedStateHandle[CREATE_ACCOUNT_BUTTON_ENABLED] = accountName.trim().isNotEmpty() && accountName.count() <= ACCOUNT_NAME_MAX_LENGTH
    }

    fun onAccountCreateClick(isWithLedger: Boolean) {
        viewModelScope.launch {
            if (state.value.isFirstAccount) { // if first time then we need authentication in order to create BDFS
                sendEvent(CreateAccountEvent.RequestBiometricAuthForFirstAccount(isWithLedger = isWithLedger))
                return@launch
            }

            accessFactorSourceForAccountCreation(
                isFirstAccount = false,
                isWithLedger = isWithLedger
            )
        }
    }

    fun handleNewProfileCreation(
        isAuthenticated: Boolean,
        isWithLedger: Boolean
    ) {
        viewModelScope.launch {
            if (isAuthenticated) {
                if (!getProfileUseCase.isInitialized()) {
                    mnemonicRepository.createNew().mapCatching { newMnemonic ->
                        generateProfileUseCase(mnemonicWithPassphrase = newMnemonic)
                        // Since we choose to create a new profile, this is the time
                        // we discard the data copied from the cloud backup, since they represent
                        // a previous instance.
                        discardTemporaryRestoredFileForBackupUseCase(BackupType.DeprecatedCloud)
                    }.onSuccess {
                        accessFactorSourceForAccountCreation(
                            isFirstAccount = true,
                            isWithLedger = isWithLedger
                        )
                    }.onFailure { throwable ->
                        handleAccountCreationError(throwable)
                        return@launch
                    }
                }
            }
        }
    }

    // when called the access factor source bottom sheet dialog is presented
    private fun accessFactorSourceForAccountCreation(
        isFirstAccount: Boolean,
        isWithLedger: Boolean
    ) {
        accessFactorSourcesJob?.cancel()
        accessFactorSourcesJob = viewModelScope.launch {
            val selectedFactorSource = if (isWithLedger) { // then get the selected ledger device
                sendEvent(CreateAccountEvent.AddLedgerDevice)
                appEventBus.events
                    .filterIsInstance<AppEvent.AccessFactorSources.SelectedLedgerDevice>()
                    .first()
                    .ledgerFactorSource
            } else {
                getProfileUseCase().mainBabylonFactorSource ?: return@launch
            }

            accessFactorSourcesProxy.getPublicKeyAndDerivationPathForFactorSource(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToDerivePublicKey(
                    forNetworkId = args.networkIdToSwitch ?: getProfileUseCase().currentGateway.network.id,
                    factorSource = selectedFactorSource,
                    isBiometricsProvided = isFirstAccount
                )
            ).onSuccess {
                handleAccountCreation { nameOfAccount ->
                    val factorSourceId = when (selectedFactorSource) {
                        is FactorSource.Device -> selectedFactorSource.value.id.asGeneral()
                        is FactorSource.Ledger -> selectedFactorSource.value.id.asGeneral()
                    }
                    createAccountUseCase(
                        displayName = DisplayName(nameOfAccount),
                        factorSourceId = factorSourceId,
                        hdPublicKey = it.value
                    )
                }
            }.onFailure { throwable ->
                handleAccountCreationError(throwable)
            }
        }
    }

    private suspend fun handleAccountCreationError(throwable: Throwable) {
        if (throwable is ProfileException.SecureStorageAccess) {
            appEventBus.sendEvent(AppEvent.SecureFolderWarning)
        } else {
            _state.update { state ->
                if (throwable is ProfileException.NoMnemonic) {
                    state.copy(shouldShowNoMnemonicError = true)
                } else {
                    if (throwable is CancellationException) { // user cancelled, don't print it
                        return
                    }
                    state.copy(
                        uiMessage = UiMessage.ErrorMessage(throwable)
                    )
                }
            }
        }
    }

    fun onBackClick() = viewModelScope.launch {
        if (!getProfileUseCase.isInitialized()) {
            deleteNotInitializedProfileDataUseCase()
        }
        sendEvent(CreateAccountEvent.Dismiss)
    }

    fun onUseLedgerSelectionChanged(selected: Boolean) {
        _state.update { it.copy(isWithLedger = selected) }
    }

    fun onUiMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    private suspend fun handleAccountCreation(
        accountProvider: suspend (String) -> Account
    ) {
        _state.update { it.copy(loading = true) }
        val accountName = accountName.value.trim()
        if (args.networkIdToSwitch != null && args.networkUrl != null) {
            switchNetworkUseCase(args.networkUrl)
        }
        val account = accountProvider(accountName)
        val accountId = account.address

        _state.update {
            it.copy(
                loading = true,
                accountId = accountId.string,
                accountName = accountName
            )
        }

        checkAndHandleFirstTimeAccountCreationExtras()

        sendEvent(
            CreateAccountEvent.Complete(
                accountId = accountId,
                requestSource = args.requestSource
            )
        )
    }

    private suspend fun checkAndHandleFirstTimeAccountCreationExtras() {
        if (args.requestSource == CreateAccountRequestSource.FirstTimeWithCloudBackupDisabled ||
            args.requestSource == CreateAccountRequestSource.FirstTimeWithCloudBackupEnabled
        ) {
            preferencesManager.setRadixBannerVisibility(isVisible = true)

            val isCloudBackupEnabled = args.requestSource == CreateAccountRequestSource.FirstTimeWithCloudBackupEnabled
            changeBackupSettingUseCase(isChecked = isCloudBackupEnabled)
        }
    }

    data class CreateAccountUiState(
        val loading: Boolean = false,
        val isFirstAccount: Boolean = false,
        val accountId: String = "",
        val accountName: String = "",
        val isWithLedger: Boolean = false,
        val uiMessage: UiMessage? = null,
        val shouldShowNoMnemonicError: Boolean = false
    ) : UiState

    fun dismissNoMnemonicError() {
        _state.update { it.copy(shouldShowNoMnemonicError = false) }
    }

    companion object {
        private const val ACCOUNT_NAME = "account_name"
        private const val IS_ACCOUNT_NAME_LENGTH_MORE_THAN_THE_MAX = "is_account_name_length_more_than_the_max"
        private const val CREATE_ACCOUNT_BUTTON_ENABLED = "create_account_button_enabled"
    }
}

internal sealed interface CreateAccountEvent : OneOffEvent {
    data class Complete(
        val accountId: AccountAddress,
        val requestSource: CreateAccountRequestSource?,
    ) : CreateAccountEvent

    data class RequestBiometricAuthForFirstAccount(val isWithLedger: Boolean) : CreateAccountEvent

    data object AddLedgerDevice : CreateAccountEvent
    data object Dismiss : CreateAccountEvent
}
