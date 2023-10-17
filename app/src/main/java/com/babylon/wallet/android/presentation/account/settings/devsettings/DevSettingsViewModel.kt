package com.babylon.wallet.android.presentation.account.settings.devsettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.usecases.FaucetState
import com.babylon.wallet.android.domain.usecases.GetFreeXrdUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.extensions.hasAuthSigning
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.AddAuthSigningFactorInstanceUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class DevSettingsViewModel @Inject constructor(
    private val getFreeXrdUseCase: GetFreeXrdUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val rolaClient: ROLAClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val addAuthSigningFactorInstanceUseCase: AddAuthSigningFactorInstanceUseCase,
    private val transactionStatusClient: TransactionStatusClient,
    @ApplicationScope private val appScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
    private val appEventBus: AppEventBus,
) : StateViewModel<DevSettingsUiState>() {

    private val args = DevSettingsArgs(savedStateHandle)
    private var createAndUploadAuthKeyJob: Job? = null

    override fun initialState(): DevSettingsUiState = DevSettingsUiState(
        accountAddress = args.address,
    )

    init {
        loadAccount()
        viewModelScope.launch {
            rolaClient.signingState.collect { signingState ->
                _state.update { state ->
                    state.copy(interactionState = signingState)
                }
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
            getProfileUseCase.accountsOnCurrentNetwork.mapNotNull { accounts -> accounts.firstOrNull { it.address == args.address } }
                .collect { account ->
                    _state.update { state ->
                        state.copy(
                            account = account,
                            hasAuthKey = account.hasAuthSigning()
                        )
                    }
                }
        }
    }

    fun onGetFreeXrdClick() {
        if (state.value.faucetState !is FaucetState.Available) return

        appScope.launch {
            _state.update { it.copy(isFreeXRDLoading = true) }
            getFreeXrdUseCase(address = args.address).onSuccess { _ ->
                _state.update { it.copy(isFreeXRDLoading = false) }
                appEventBus.sendEvent(AppEvent.RefreshResourcesNeeded)
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

    fun onCreateAndUploadAuthKey() {
        createAndUploadAuthKeyJob = viewModelScope.launch {
            state.value.account?.let { account ->
                _state.update { it.copy(isLoading = true) }
                rolaClient.generateAuthSigningFactorInstance(account).onSuccess { authSigningFactorInstance ->
                    val manifest = rolaClient
                        .createAuthKeyManifestWithStringInstructions(account, authSigningFactorInstance)
                        .getOrElse {
                            _state.update { state ->
                                state.copy(isLoading = false)
                            }
                            return@launch
                        }
                    Timber.d("Approving: \n$manifest")
                    val interactionId = UUIDGenerator.uuid().toString()
                    incomingRequestRepository.add(
                        manifest.prepareInternalTransactionRequest(
                            networkId = account.networkID,
                            requestId = interactionId,
                            blockUntilCompleted = true,
                            transactionType = TransactionType.CreateRolaKey(authSigningFactorInstance)
                        )
                    )
                    _state.update { it.copy(isLoading = false) }
                    listenForRolaKeyUploadTransactionResult(interactionId)
                }.onFailure {
                    _state.update { state ->
                        state.copy(isLoading = false)
                    }
                }
            }
        }
    }

    private fun listenForRolaKeyUploadTransactionResult(requestId: String) {
        viewModelScope.launch {
            transactionStatusClient.listenForPollStatusByRequestId(requestId).collect { status ->
                status.result.onSuccess {
                    transactionStatusClient.statusHandled(status.txId)
                    when (val type = status.transactionType) {
                        is TransactionType.CreateRolaKey -> {
                            val account = requireNotNull(state.value.account)
                            addAuthSigningFactorInstanceUseCase(account, type.factorInstance)
                        }

                        else -> {}
                    }
                }
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}

data class DevSettingsUiState(
    val account: Network.Account? = null,
    val accountAddress: String,
    val faucetState: FaucetState = FaucetState.Unavailable,
    val isFreeXRDLoading: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
    val hasAuthKey: Boolean = false,
    val interactionState: InteractionState? = null,
) : UiState
