package com.babylon.wallet.android.presentation.account.createaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
import com.babylon.wallet.android.domain.usecases.DeleteWalletUseCase
import com.babylon.wallet.android.domain.usecases.SyncAccountThirdPartyDepositsWithLedger
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
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.extensions.SharedConstants.entityNameMaxLength
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.isManualCancellation
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.KeystoreManager
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.FirstAccountCreationStatusManager
import rdx.works.profile.domain.GetProfileUseCase
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
    private val firstAccountCreationStatusManager: FirstAccountCreationStatusManager,
    private val syncAccountThirdPartyDepositsWithLedger: SyncAccountThirdPartyDepositsWithLedger,
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

    override fun initialState(): CreateAccountUiState = CreateAccountUiState(
        isFirstAccount = args.requestSource?.isFirstTime() == true
    )

    fun onAccountNameChange(accountName: String) {
        _state.update { it.copy(accountName = accountName) }
    }

    fun onAccountCreateClick(isWithLedger: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isCreatingAccount = true) }

            if (shouldCreateWallet()) {
                val walletCreated = createWallet().isSuccess
                if (!walletCreated) return@launch
            }

            val factorSourceToCreateAccount = resolveFactorSource(isWithLedger = isWithLedger) ?: run {
                _state.update { it.copy(isCreatingAccount = false) }
                return@launch
            }

            val networkId = args.networkIdToSwitch ?: getProfileUseCase().currentGateway.network.id
            val name = DisplayName.init(state.value.accountName.trim())

            if (args.requestSource?.isFirstTime() == true) {
                firstAccountCreationStatusManager.onFirstAccountCreationInProgress()
            }

            when (factorSourceToCreateAccount) {
                is FactorSourceToCreateAccount.Ledger -> runCatching {
                    sargonOsManager.sargonOs.createAndSaveNewAccountWithFactorSource(
                        factorSource = factorSourceToCreateAccount.factorSource,
                        networkId = networkId,
                        name = name
                    )
                }

                is FactorSourceToCreateAccount.MainBabylon -> runCatching {
                    sargonOsManager.sargonOs.createAndSaveNewAccountWithMainBdfs(
                        networkId = networkId,
                        name = name
                    )
                }
            }.onSuccess { account ->
                onAccountCreated(account = account)
            }.onFailure { error ->
                Timber.w(error)
                _state.update { it.copy(isCreatingAccount = false) }

                if (args.requestSource?.isFirstTime() == true) {
                    firstAccountCreationStatusManager.onFirstAccountCreationAborted()
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

    fun dismissNoMnemonicError() {
        _state.update { it.copy(shouldShowNoMnemonicError = false) }
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

    private suspend fun createWallet(): Result<Unit> {
        val sargonOs = sargonOsManager.sargonOs
        keystoreManager.resetMnemonicKeySpecWhenInvalidated()

        return runCatching {
            sargonOs.newWallet(shouldPreDeriveInstances = false)
        }.onSuccess {
            // Since we choose to create a new profile, this is the time
            // we discard the data copied from the cloud backup, since they represent
            // a previous instance.
            discardTemporaryRestoredFileForBackupUseCase(BackupType.DeprecatedCloud)
        }.onFailure { profileCreationError ->
            if (profileCreationError is CommonException.SecureStorageAccessException) {
                if (!profileCreationError.errorKind.isManualCancellation()) {
                    _state.update {
                        it.copy(isCreatingAccount = false, uiMessage = UiMessage.ErrorMessage(profileCreationError))
                    }
                } else {
                    _state.update {
                        it.copy(isCreatingAccount = false)
                    }
                }
            } else if (profileCreationError is CommonException.SecureStorageWriteException) {
                appEventBus.sendEvent(AppEvent.SecureFolderWarning)
            } else {
                _state.update {
                    it.copy(isCreatingAccount = false, uiMessage = UiMessage.ErrorMessage(profileCreationError))
                }
            }
        }
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

    private suspend fun resolveFactorSource(isWithLedger: Boolean): FactorSourceToCreateAccount? = if (isWithLedger) {
        sendEvent(CreateAccountEvent.AddLedgerDevice)
        val event = appEventBus.events
            .filterIsInstance<AppEvent.AccessFactorSources.SelectLedgerOutcome>()
            .first()

        when (event) {
            is AppEvent.AccessFactorSources.SelectLedgerOutcome.Rejected -> null
            is AppEvent.AccessFactorSources.SelectLedgerOutcome.Selected -> FactorSourceToCreateAccount.Ledger(event.ledgerFactorSource)
        }
    } else {
        FactorSourceToCreateAccount.MainBabylon
    }

    private suspend fun onAccountCreated(account: Account) {
        if (args.networkIdToSwitch != null) {
            switchNetworkUseCase(networkId = args.networkIdToSwitch)
        }

        syncAccountThirdPartyDepositsWithLedger(account = account)
        checkAndHandleFirstTimeAccountCreationExtras()

        _state.update {
            it.copy(
                isCreatingAccount = false,
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
        firstAccountCreationStatusManager.onFirstAccountCreationConfirmed()
    }

    private sealed interface FactorSourceToCreateAccount {
        data object MainBabylon : FactorSourceToCreateAccount

        data class Ledger(val factorSource: FactorSource.Ledger) : FactorSourceToCreateAccount
    }

    data class CreateAccountUiState(
        val isCreatingAccount: Boolean = false,
        val isFirstAccount: Boolean = false,
        val accountId: String = "",
        val accountName: String = "",
        val isWithLedger: Boolean = false,
        val uiMessage: UiMessage? = null,
        val shouldShowNoMnemonicError: Boolean = false
    ) : UiState {

        private val accountNameTrimmed: String
            get() = accountName.trim()

        val isAccountNameErrorVisible: Boolean
            get() = accountNameTrimmed.isNotEmpty() && accountNameTrimmed.count() > entityNameMaxLength

        val isSubmitEnabled: Boolean
            get() = accountNameTrimmed.isNotEmpty() && accountNameTrimmed.count() <= entityNameMaxLength
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
