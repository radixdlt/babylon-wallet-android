package com.babylon.wallet.android.presentation.account.createaccount

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
import com.babylon.wallet.android.utils.Constants.ACCOUNT_NAME_MAX_LENGTH
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.DeleteProfileUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.account.SwitchNetworkUseCase
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.ChangeBackupSettingUseCase
import rdx.works.profile.domain.backup.DiscardTemporaryRestoredFileForBackupUseCase
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class CreateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val createAccountUseCase: CreateAccountUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
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

    override fun initialState(): CreateAccountUiState = CreateAccountUiState(
        firstTime = args.requestSource?.isFirstTime() == true
    )

    fun onAccountNameChange(accountName: String) {
        savedStateHandle[ACCOUNT_NAME] = accountName
        savedStateHandle[IS_ACCOUNT_NAME_LENGTH_MORE_THAN_THE_MAX] = accountName.count() > ACCOUNT_NAME_MAX_LENGTH
        savedStateHandle[CREATE_ACCOUNT_BUTTON_ENABLED] = accountName.trim().isNotEmpty() && accountName.count() <= ACCOUNT_NAME_MAX_LENGTH
    }

    fun onAccountCreateClick(isWithLedger: Boolean, biometricAuthProvider: suspend () -> Boolean) {
        viewModelScope.launch {
            if (biometricAuthProvider().not()) {
                return@launch
            }
            if (!getProfileUseCase.isInitialized()) {
                mnemonicRepository.createNew().mapCatching { newMnemonic ->
                    generateProfileUseCase(mnemonicWithPassphrase = newMnemonic)
                    discardTemporaryRestoredFileForBackupUseCase(BackupType.Cloud)
                }.onFailure { throwable ->
                    handleAccountCreationError(throwable)
                    return@launch
                }
            }

            // at the moment you can create a account either with device factor source or ledger factor source
            val selectedFactorSource = if (isWithLedger) { // get the selected ledger device
                sendEvent(CreateAccountEvent.AddLedgerDevice)
                appEventBus.events
                    .filterIsInstance<AppEvent.AccessFactorSources.SelectedLedgerDevice>()
                    .first()
                    .ledgerFactorSource
            } else {
                getProfileUseCase().mainBabylonFactorSource ?: return@launch
            }

            // if main babylon factor source is not present, it will be created during the public key derivation
            accessFactorSourcesProxy.getPublicKeyAndDerivationPathForFactorSource(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToDerivePublicKey(
                    forNetworkId = args.networkIdToSwitch ?: getProfileUseCase().currentGateway.network.id,
                    factorSource = selectedFactorSource,
                    isBiometricsProvided = true
                )
            ).onSuccess {
                handleAccountCreate { nameOfAccount ->
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
                    state.copy(isNoMnemonicErrorVisible = true)
                } else {
                    state.copy(
                        uiMessage = UiMessage.ErrorMessage(throwable)
                    )
                }
            }
        }
    }

    fun onBackClick() = viewModelScope.launch {
        if (!getProfileUseCase.isInitialized()) {
            deleteProfileUseCase.deleteProfileDataOnly()
        }
        sendEvent(CreateAccountEvent.Dismiss)
    }

    fun onUseLedgerSelectionChanged(selected: Boolean) {
        _state.update { it.copy(isWithLedger = selected) }
    }

    fun onUiMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    private suspend fun handleAccountCreate(
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
        val accountId: String = "",
        val accountName: String = "",
        val firstTime: Boolean = false,
        val isWithLedger: Boolean = false,
        val isCancelable: Boolean = true,
        val uiMessage: UiMessage? = null,
        val isNoMnemonicErrorVisible: Boolean = false
    ) : UiState

    fun dismissNoMnemonicError() {
        _state.update { it.copy(isNoMnemonicErrorVisible = false) }
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

    data object AddLedgerDevice : CreateAccountEvent
    data object Dismiss : CreateAccountEvent
}
