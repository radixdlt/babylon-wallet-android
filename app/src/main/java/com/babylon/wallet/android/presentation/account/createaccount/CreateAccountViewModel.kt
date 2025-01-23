package com.babylon.wallet.android.presentation.account.createaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
import com.babylon.wallet.android.domain.usecases.SyncAccountThirdPartyDepositsWithLedger
import com.babylon.wallet.android.domain.usecases.DeleteWalletUseCase
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
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.extensions.SharedConstants.entityNameMaxLength
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.isManualCancellation
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.KeystoreManager
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.updateThirdPartyDepositSettings
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.account.SwitchNetworkUseCase
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.ChangeBackupSettingUseCase
import rdx.works.profile.domain.backup.DiscardTemporaryRestoredFileForBackupUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class CreateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val syncAccountThirdPartyDepositsWithLedger: SyncAccountThirdPartyDepositsWithLedger,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val getProfileUseCase: GetProfileUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val discardTemporaryRestoredFileForBackupUseCase: DiscardTemporaryRestoredFileForBackupUseCase,
    private val switchNetworkUseCase: SwitchNetworkUseCase,
    private val changeBackupSettingUseCase: ChangeBackupSettingUseCase,
    private val appEventBus: AppEventBus,
    private val homeCardsRepository: HomeCardsRepository,
    private val sargonOsManager: SargonOsManager,
    private val keystoreManager: KeystoreManager
) : StateViewModel<CreateAccountViewModel.CreateAccountUiState>(),
    OneOffEventHandler<CreateAccountEvent> by OneOffEventHandlerImpl() {

    private val args = CreateAccountNavArgs(savedStateHandle)
    val accountName = savedStateHandle.getStateFlow(ACCOUNT_NAME, "")
    val buttonEnabled = savedStateHandle.getStateFlow(CREATE_ACCOUNT_BUTTON_ENABLED, false)
    val isAccountNameLengthMoreThanTheMax = savedStateHandle.getStateFlow(IS_ACCOUNT_NAME_LENGTH_MORE_THAN_THE_MAX, false)

    override fun initialState(): CreateAccountUiState = CreateAccountUiState(
        isFirstAccount = args.requestSource?.isFirstTime() == true
    )

    fun onAccountNameChange(accountName: String) {
        savedStateHandle[ACCOUNT_NAME] = accountName
        savedStateHandle[IS_ACCOUNT_NAME_LENGTH_MORE_THAN_THE_MAX] = accountName.count() > entityNameMaxLength
        savedStateHandle[CREATE_ACCOUNT_BUTTON_ENABLED] = accountName.trim().isNotEmpty() && accountName.count() <= entityNameMaxLength
    }

    fun onAccountCreateClick(isWithLedger: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            if (shouldCreateWallet()) {
                createWallet()
                    .then {
                        // Since we choose to create a new profile, this is the time
                        // we discard the data copied from the cloud backup, since they represent
                        // a previous instance.
                        discardTemporaryRestoredFileForBackupUseCase(BackupType.DeprecatedCloud)
                        resolveFactorSourceAndCreateAccount(isWithLedger = isWithLedger)
                    }
            } else {
                resolveFactorSourceAndCreateAccount(isWithLedger = isWithLedger)
            }.onSuccess { account ->
                if (args.networkIdToSwitch != null) {
                    switchNetworkUseCase(networkId = args.networkIdToSwitch)
                }

                syncAccountThirdPartyDepositsWithLedger(account = account)
                checkAndHandleFirstTimeAccountCreationExtras()

                _state.update {
                    it.copy(
                        loading = false,
                        accountId = account.address.string,
                        accountName = account.displayName.value
                    )
                }

                sendEvent(
                    CreateAccountEvent.Complete(
                        accountId = account.address,
                        requestSource = args.requestSource
                    )
                )
            }.onFailure { error ->
                Timber.w(error)
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private suspend fun createWallet(): Result<Unit> {
        val sargonOs = sargonOsManager.sargonOs
        keystoreManager.resetMnemonicKeySpecWhenInvalidated()

        return runCatching {
            sargonOs.newWallet(
                shouldPreDeriveInstances = false
            )
        }.onFailure { profileCreationError ->
            if (profileCreationError is CommonException.SecureStorageAccessException) {
                if (!profileCreationError.errorKind.isManualCancellation()) {
                    _state.update {
                        it.copy(uiMessage = UiMessage.ErrorMessage(profileCreationError))
                    }
                }
            } else if (profileCreationError is CommonException.SecureStorageWriteException) {
                appEventBus.sendEvent(AppEvent.SecureFolderWarning)
            } else {
                _state.update {
                    it.copy(uiMessage = UiMessage.ErrorMessage(profileCreationError))
                }
            }
        }
    }

    private suspend fun resolveFactorSourceAndCreateAccount(isWithLedger: Boolean): Result<Account> {
        val networkId = args.networkIdToSwitch ?: getProfileUseCase().currentGateway.network.id

        val nonMainFactorSource = if (isWithLedger) {
            sendEvent(CreateAccountEvent.AddLedgerDevice)
            appEventBus.events
                .filterIsInstance<AppEvent.AccessFactorSources.SelectedLedgerDevice>()
                .first()
                .ledgerFactorSource
        } else {
            null
        }

        val name = DisplayName.init(accountName.value.trim())
        return runCatching {
            if (nonMainFactorSource != null) {
                sargonOsManager.sargonOs.createAndSaveNewAccountWithFactorSource(
                    factorSource = nonMainFactorSource,
                    networkId = networkId,
                    name = name
                )
            } else {
                sargonOsManager.sargonOs.createAndSaveNewAccountWithBdfs(
                    networkId = networkId,
                    name = name
                )
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
        if (args.networkIdToSwitch != null) {
            switchNetworkUseCase(args.networkIdToSwitch)
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

    private suspend fun shouldCreateWallet(): Boolean {
        // If profile with networks exists, then no need to create a new one.
        if (getProfileUseCase.finishedOnboardingProfile() != null) {
            return false
        }

        // Delete the instance of the old one, since it might have been an ephemeral profile
        // created during onboarding while creating an account with ledger, but user killed the app in the meantime.
        deleteWalletUseCase()
        return true
    }

    private suspend fun checkAndHandleFirstTimeAccountCreationExtras() {
        if (args.requestSource == CreateAccountRequestSource.FirstTimeWithCloudBackupDisabled ||
            args.requestSource == CreateAccountRequestSource.FirstTimeWithCloudBackupEnabled
        ) {
            homeCardsRepository.walletCreated()
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

    data object AddLedgerDevice : CreateAccountEvent
    data object Dismiss : CreateAccountEvent
}
