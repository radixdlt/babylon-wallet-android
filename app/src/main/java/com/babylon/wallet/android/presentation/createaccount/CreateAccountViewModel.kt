package com.babylon.wallet.android.presentation.createaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.AppConstants.ACCOUNT_NAME_MAX_LENGTH
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.decodeUtf8
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetProfileStateUseCase
import rdx.works.profile.domain.account.CreateAccountWithDeviceFactorSourceUseCase
import rdx.works.profile.domain.account.CreateAccountWithLedgerFactorSourceUseCase
import rdx.works.profile.domain.account.SwitchNetworkUseCase
import rdx.works.profile.domain.exists
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class CreateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getProfileStateUseCase: GetProfileStateUseCase,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val createAccountWithDeviceFactorSourceUseCase: CreateAccountWithDeviceFactorSourceUseCase,
    private val createAccountWithLedgerFactorSourceUseCase: CreateAccountWithLedgerFactorSourceUseCase,
    private val switchNetworkUseCase: SwitchNetworkUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<CreateAccountViewModel.CreateAccountState>(),
    OneOffEventHandler<CreateAccountEvent> by OneOffEventHandlerImpl() {

    private val args = CreateAccountNavArgs(savedStateHandle)
    val accountName = savedStateHandle.getStateFlow(ACCOUNT_NAME, "")
    val buttonEnabled = savedStateHandle.getStateFlow(CREATE_ACCOUNT_BUTTON_ENABLED, false)

    init {
        viewModelScope.launch {
            if (!getProfileStateUseCase.exists()) {
                generateProfileUseCase()
            }
        }
        viewModelScope.launch {
            appEventBus.events
                .filterIsInstance<AppEvent.DerivedAccountPublicKeyWithLedger>()
                .collect {
                    createLedgerAccount(
                        factorSourceID = it.factorSourceID,
                        derivationPath = it.derivationPath,
                        derivedPublicKeyHex = it.derivedPublicKeyHex
                    )
                }
        }
    }

    private suspend fun createLedgerAccount(
        factorSourceID: FactorSource.FactorSourceID.FromHash,
        derivationPath: DerivationPath,
        derivedPublicKeyHex: String
    ) {
        viewModelScope.launch {
            handleAccountCreate { accountName, networkId ->
                createAccountWithLedgerFactorSourceUseCase(
                    displayName = accountName,
                    networkID = networkId,
                    derivedPublicKeyHex = derivedPublicKeyHex,
                    ledgerFactorSourceID = factorSourceID,
                    derivationPath = derivationPath
                )
            }
        }
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

        sendEvent(
            CreateAccountEvent.Complete(
                accountId = accountId,
                args.requestSource
            )
        )
    }

    override fun initialState(): CreateAccountState = CreateAccountState(
        firstTime = args.requestSource == CreateAccountRequestSource.FirstTime
    )

    fun onAccountNameChange(accountName: String) {
        savedStateHandle[ACCOUNT_NAME] = accountName.take(ACCOUNT_NAME_MAX_LENGTH)
        savedStateHandle[CREATE_ACCOUNT_BUTTON_ENABLED] = accountName.trim().isNotEmpty()
    }

    fun onAccountCreateClick() {
        viewModelScope.launch {
            if (state.value.useLedgerSelected) {
                sendEvent(CreateAccountEvent.AddLedgerDevice)
            } else {
                handleAccountCreate { name, networkId ->
                    createAccountWithDeviceFactorSourceUseCase(
                        displayName = name,
                        networkID = networkId
                    )
                }
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private suspend fun switchNetworkIfNeeded(): NetworkId? {
        val switchNetwork = args.switchNetwork ?: false
        var networkId: NetworkId? = null
        if (switchNetwork) {
            val networkUrl = args.networkUrlEncoded!!.decodeUtf8()
            val networkName = args.networkName!!
            networkId = switchNetworkUseCase(networkUrl, networkName)
        }
        return networkId
    }

    fun onUseLedgerSelectionChanged(selected: Boolean) {
        _state.update { it.copy(useLedgerSelected = selected) }
    }

    data class CreateAccountState(
        val loading: Boolean = false,
        val accountId: String = "",
        val accountName: String = "",
        val firstTime: Boolean = false,
        val useLedgerSelected: Boolean = false
    ) : UiState

    companion object {
        private const val ACCOUNT_NAME = "account_name"
        private const val CREATE_ACCOUNT_BUTTON_ENABLED = "create_account_button_enabled"
    }
}

internal sealed interface CreateAccountEvent : OneOffEvent {
    data class Complete(
        val accountId: String,
        val requestSource: CreateAccountRequestSource?,
    ) : CreateAccountEvent

    object AddLedgerDevice : CreateAccountEvent
}
