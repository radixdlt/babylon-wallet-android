package com.babylon.wallet.android.presentation.account.createaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.transaction.InteractionState
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.mainBabylonFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.derivation.model.NetworkId
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

            val onNetworkId = if (args.networkId != -1) {
                NetworkId.from(args.networkId)
            } else {
                val profile = getProfileUseCase.invoke().first()
                profile.currentNetwork?.knownNetworkId ?: Radix.Gateway.default.network.networkId()
            }

            // at the moment you can create a account either with device factor source or ledger factor source
            var selectedFactorSource: FactorSource.CreatingEntity? = null

            if (isWithLedger) { // get the selected ledger device
                sendEvent(CreateAccountEvent.AddLedgerDevice)
                selectedFactorSource = appEventBus.events
                    .filterIsInstance<AppEvent.AccessFactorSources.SelectedLedgerDevice>()
                    .first().ledgerFactorSource
            }

            // if main babylon factor source is not present, it will be created during the public key derivation
            accessFactorSourcesProxy.getPublicKeyAndDerivationPathForFactorSource(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToCreateAccount(
                    forNetworkId = onNetworkId,
                    factorSource = if (isWithLedger && selectedFactorSource != null) {
                        selectedFactorSource
                    } else {
                        null
                    }
                )
            ).onSuccess {
                handleAccountCreate { nameOfAccount, networkId ->
                    // when we reach this point main babylon factor source has already created
                    if (selectedFactorSource == null && isWithLedger.not()) { // so take it if it is a creation with device
                        val profile = getProfileUseCase.invoke().first() // get again the profile with its updated state
                        selectedFactorSource = profile.mainBabylonFactorSource() ?: error("Babylon factor source is not present")
                    }
                    createAccountUseCase(
                        displayName = nameOfAccount,
                        factorSource = selectedFactorSource ?: error("factor source must not be null"),
                        publicKeyAndDerivationPath = it,
                        onNetworkId = networkId
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

    fun onDismissSigningStatusDialog() {
        _state.update { it.copy(interactionState = null) }
    }

    fun onUseLedgerSelectionChanged(selected: Boolean) {
        _state.update { it.copy(isWithLedger = selected) }
    }

    fun onUiMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    private suspend fun handleAccountCreate(
        accountProvider: suspend (String, NetworkId?) -> Network.Account
    ) {
        _state.update { it.copy(loading = true) }
        val accountName = accountName.value.trim()
        val networkId = switchNetworkIfNeeded()
        val account = accountProvider(accountName, networkId)
        val accountId = account.address

        _state.update {
            it.copy(
                loading = true,
                accountId = accountId,
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

    @Suppress("UnsafeCallOnNullableType")
    private suspend fun switchNetworkIfNeeded(): NetworkId? {
        val switchNetwork = args.switchNetwork ?: false
        var networkId: NetworkId? = null
        if (switchNetwork) {
            val networkUrl = args.networkUrlEncoded!!.decodeUtf8()
            val id = args.networkId
            networkId = switchNetworkUseCase(networkUrl, id)
        }
        return networkId
    }

    data class CreateAccountUiState(
        val loading: Boolean = false,
        val accountId: String = "",
        val accountName: String = "",
        val firstTime: Boolean = false,
        val isWithLedger: Boolean = false,
        val isCancelable: Boolean = true,
        val interactionState: InteractionState? = null,
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
        val accountId: String,
        val requestSource: CreateAccountRequestSource?,
    ) : CreateAccountEvent

    data object AddLedgerDevice : CreateAccountEvent
    data object Dismiss : CreateAccountEvent
}
