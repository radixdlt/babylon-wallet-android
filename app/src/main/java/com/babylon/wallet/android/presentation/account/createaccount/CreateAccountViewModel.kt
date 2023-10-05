package com.babylon.wallet.android.presentation.account.createaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.Constants.ACCOUNT_NAME_MAX_LENGTH
import com.babylon.wallet.android.utils.decodeUtf8
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.DeleteProfileUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetProfileStateUseCase
import rdx.works.profile.domain.account.CreateAccountWithDeviceFactorSourceUseCase
import rdx.works.profile.domain.account.CreateAccountWithLedgerFactorSourceUseCase
import rdx.works.profile.domain.account.SwitchNetworkUseCase
import rdx.works.profile.domain.isInitialized
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class CreateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getProfileStateUseCase: GetProfileStateUseCase,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val createAccountWithDeviceFactorSourceUseCase: CreateAccountWithDeviceFactorSourceUseCase,
    private val createAccountWithLedgerFactorSourceUseCase: CreateAccountWithLedgerFactorSourceUseCase,
    private val switchNetworkUseCase: SwitchNetworkUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<CreateAccountViewModel.CreateAccountState>(),
    OneOffEventHandler<CreateAccountEvent> by OneOffEventHandlerImpl() {

    private val args = CreateAccountNavArgs(savedStateHandle)
    val accountName = savedStateHandle.getStateFlow(ACCOUNT_NAME, "")
    val buttonEnabled = savedStateHandle.getStateFlow(CREATE_ACCOUNT_BUTTON_ENABLED, false)
    val isAccountNameLengthMoreThanTheMax = savedStateHandle.getStateFlow(IS_ACCOUNT_NAME_LENGTH_MORE_THAN_THE_MAX, false)

    init {
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
        firstTime = args.requestSource?.isFirstTime() == true,
        isCancelable = true,
    )

    fun onAccountNameChange(accountName: String) {
        savedStateHandle[ACCOUNT_NAME] = accountName // .take(ACCOUNT_NAME_MAX_LENGTH)
        savedStateHandle[IS_ACCOUNT_NAME_LENGTH_MORE_THAN_THE_MAX] = accountName.count() > ACCOUNT_NAME_MAX_LENGTH
        savedStateHandle[CREATE_ACCOUNT_BUTTON_ENABLED] = accountName.trim().isNotEmpty() && accountName.count() <= ACCOUNT_NAME_MAX_LENGTH
    }

    fun onAccountCreateClick() {
        viewModelScope.launch {
            if (!getProfileStateUseCase.isInitialized()) {
                generateProfileUseCase()
            }

            if (state.value.useLedgerSelected) {
                sendEvent(CreateAccountEvent.AddLedgerDevice(args.networkId))
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

    fun onBackClick() = viewModelScope.launch {
        if (!state.value.isCancelable) return@launch

        if (!getProfileStateUseCase.isInitialized()) {
            deleteProfileUseCase()
        }
        sendEvent(CreateAccountEvent.Dismiss)
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

    fun onUseLedgerSelectionChanged(selected: Boolean) {
        _state.update { it.copy(useLedgerSelected = selected) }
    }

    data class CreateAccountState(
        val loading: Boolean = false,
        val accountId: String = "",
        val accountName: String = "",
        val firstTime: Boolean = false,
        val useLedgerSelected: Boolean = false,
        val isCancelable: Boolean = true
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

    data class AddLedgerDevice(val networkId: Int) : CreateAccountEvent
    data object Dismiss : CreateAccountEvent
}
