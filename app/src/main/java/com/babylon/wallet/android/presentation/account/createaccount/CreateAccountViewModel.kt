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
import com.babylon.wallet.android.utils.decodeUtf8
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Url
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
import rdx.works.profile.domain.DeleteProfileUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetProfileStateUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.SwitchNetworkUseCase
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.DiscardTemporaryRestoredFileForBackupUseCase
import rdx.works.profile.domain.isInitialized
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class CreateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val createAccountUseCase: CreateAccountUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfileStateUseCase: GetProfileStateUseCase,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val discardTemporaryRestoredFileForBackupUseCase: DiscardTemporaryRestoredFileForBackupUseCase,
    private val preferencesManager: PreferencesManager,
    private val switchNetworkUseCase: SwitchNetworkUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<CreateAccountViewModel.CreateAccountUiState>(),
    OneOffEventHandler<CreateAccountEvent> by OneOffEventHandlerImpl() {

    private val args = CreateAccountNavArgs(savedStateHandle)
    val accountName = savedStateHandle.getStateFlow(ACCOUNT_NAME, "")
    val buttonEnabled = savedStateHandle.getStateFlow(CREATE_ACCOUNT_BUTTON_ENABLED, false)
    val isAccountNameLengthMoreThanTheMax = savedStateHandle.getStateFlow(IS_ACCOUNT_NAME_LENGTH_MORE_THAN_THE_MAX, false)

    override fun initialState(): CreateAccountUiState = CreateAccountUiState(
        firstTime = args.requestSource?.isFirstTime() == true,
        isCancelable = true,
    )

    fun onAccountNameChange(accountName: String) {
        savedStateHandle[ACCOUNT_NAME] = accountName
        savedStateHandle[IS_ACCOUNT_NAME_LENGTH_MORE_THAN_THE_MAX] = accountName.count() > ACCOUNT_NAME_MAX_LENGTH
        savedStateHandle[CREATE_ACCOUNT_BUTTON_ENABLED] = accountName.trim().isNotEmpty() && accountName.count() <= ACCOUNT_NAME_MAX_LENGTH
    }

    fun onAccountCreateClick(isWithLedger: Boolean) {
        viewModelScope.launch {
            if (!getProfileStateUseCase.isInitialized()) {
                generateProfileUseCase()
                // Since we choose to create a new profile, this is the time
                // we discard the data copied from the cloud backup, since they represent
                // a previous instance.
                discardTemporaryRestoredFileForBackupUseCase(BackupType.Cloud)
            }

            val onNetworkId = args.networkIdToSwitch ?: getProfileUseCase().currentGateway.network.id

            // at the moment you can create a account either with device factor source or ledger factor source
            var selectedFactorSource: FactorSource? = null

            if (isWithLedger) { // get the selected ledger device
                sendEvent(CreateAccountEvent.AddLedgerDevice)
                selectedFactorSource = appEventBus.events
                    .filterIsInstance<AppEvent.AccessFactorSources.SelectedLedgerDevice>()
                    .first()
                    .ledgerFactorSource
            }

            // if main babylon factor source is not present, it will be created during the public key derivation
            accessFactorSourcesProxy.getPublicKeyAndDerivationPathForFactorSource(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToDerivePublicKey(
                    forNetworkId = onNetworkId,
                    factorSource = if (isWithLedger && selectedFactorSource != null) {
                        selectedFactorSource
                    } else {
                        null
                    }
                )
            ).onSuccess {
                handleAccountCreate { nameOfAccount ->
                    // when we reach this point main babylon factor source has already created
                    if (selectedFactorSource == null && isWithLedger.not()) { // so take it if it is a creation with device
                        val profile = getProfileUseCase() // get again the profile with its updated state
                        selectedFactorSource = profile.mainBabylonFactorSource ?: error("Babylon factor source is not present")
                    }

                    val factorSourceId = when (val factorSource = selectedFactorSource) {
                        is FactorSource.Device -> factorSource.value.id.asGeneral()
                        is FactorSource.Ledger -> factorSource.value.id.asGeneral()
                        null -> error("factor source must not be null")
                    }
                    createAccountUseCase(
                        displayName = DisplayName(nameOfAccount),
                        factorSourceId = factorSourceId,
                        publicKeyAndDerivationPath = it
                    )
                }
            }.onFailure { error ->
                _state.update { state ->
                    state.copy(
                        uiMessage = UiMessage.ErrorMessage(error)
                    )
                }
            }
        }
    }

    fun onBackClick() = viewModelScope.launch {
        if (!state.value.isCancelable) return@launch

        if (!getProfileStateUseCase.isInitialized()) {
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
        if (args.networkIdToSwitch != null) {
            switchNetworkUseCase(args.networkUrl!!)
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

        if (args.requestSource == CreateAccountRequestSource.FirstTime) {
            preferencesManager.setRadixBannerVisibility(isVisible = true)
        }

        sendEvent(
            CreateAccountEvent.Complete(
                accountId = accountId,
                requestSource = args.requestSource
            )
        )
    }

    data class CreateAccountUiState(
        val loading: Boolean = false,
        val accountId: String = "",
        val accountName: String = "",
        val firstTime: Boolean = false,
        val isWithLedger: Boolean = false,
        val isCancelable: Boolean = true,
        val uiMessage: UiMessage? = null
    ) : UiState

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
