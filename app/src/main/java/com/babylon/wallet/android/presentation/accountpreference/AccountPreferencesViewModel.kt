package com.babylon.wallet.android.presentation.accountpreference

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.usecases.GetFreeXrdUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.utils.hasAuthSigning
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.AddAuthSigningFactorInstanceUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class AccountPreferenceViewModel @Inject constructor(
    private val getFreeXrdUseCase: GetFreeXrdUseCase,
    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val getProfileUseCase: GetProfileUseCase,
    private val rolaClient: ROLAClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val addAuthSigningFactorInstanceUseCase: AddAuthSigningFactorInstanceUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
    private val appEventBus: AppEventBus
) : StateViewModel<AccountPreferenceUiState>() {

    private val args = AccountPreferencesArgs(savedStateHandle)
    private var authSigningFactorInstance: FactorInstance? = null
    private lateinit var uploadAuthKeyRequestId: String
    private var job: Job? = null

    override fun initialState(): AccountPreferenceUiState = AccountPreferenceUiState(accountAddress = args.address)

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
            appEventBus.events.filterIsInstance<AppEvent.Status.Transaction>().filter { it.requestId == uploadAuthKeyRequestId }
                .collect { event ->
                    when (event) {
                        is AppEvent.Status.Transaction.Fail -> {
                            _state.update { it.copy(isLoading = false) }
                        }

                        is AppEvent.Status.Transaction.Success -> {
                            val account = requireNotNull(state.value.account)
                            val authSigningFactorInstance = requireNotNull(authSigningFactorInstance)
                            addAuthSigningFactorInstanceUseCase(account, authSigningFactorInstance)
                            _state.update { it.copy(isLoading = false) }
                        }

                        else -> {}
                    }
                }
        }
        viewModelScope.launch {
            getFreeXrdUseCase.isAllowedToUseFaucet(args.address).collect { isAllowed ->
                _state.update {
                    it.copy(
                        canUseFaucet = isAllowed,
                        isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
                    )
                }
            }
        }
    }

    private fun loadAccount() {
        viewModelScope.launch {
            getProfileUseCase.accountsOnCurrentNetwork.map { accounts -> accounts.first { it.address == args.address } }
                .collect { account ->
                    _state.update {
                        it.copy(
                            account = account,
                            isDeviceSecure = deviceSecurityHelper.isDeviceSecure(),
                            hasAuthKey = account.hasAuthSigning()
                        )
                    }
                }
        }
    }

    fun onGetFreeXrdClick() {
        appScope.launch {
            _state.update { it.copy(isLoading = true) }
            getFreeXrdUseCase(address = args.address).onSuccess { _ ->
                _state.update { it.copy(isLoading = false, gotFreeXrd = true) }
                appEventBus.sendEvent(AppEvent.GotFreeXrd)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiMessage.ErrorMessage.from(error = error)
                    )
                }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    fun onDismissSigning() {
        job?.cancel()
        job = null
    }

    fun onCreateAndUploadAuthKey() {
        job = viewModelScope.launch {
            state.value.account?.let { account ->
                _state.update { it.copy(isLoading = true) }
                rolaClient.generateAuthSigningFactorInstance(account).onSuccess { authSigningFactorInstance ->
                    this@AccountPreferenceViewModel.authSigningFactorInstance = authSigningFactorInstance
                    val manifest = rolaClient.createAuthKeyManifestWithStringInstructions(account, authSigningFactorInstance)
                    Timber.d("Approving: \n$manifest")
                    uploadAuthKeyRequestId = UUIDGenerator.uuid().toString()
                    incomingRequestRepository.add(
                        manifest.prepareInternalTransactionRequest(
                            networkId = account.networkID,
                            requestId = uploadAuthKeyRequestId
                        )
                    )
                    _state.update { it.copy(isLoading = false) }
                }.onFailure {
                    _state.update { state ->
                        state.copy(isLoading = false)
                    }
                }
            }
        }
    }
}

data class AccountPreferenceUiState(
    val account: Network.Account? = null,
    val accountAddress: String,
    val canUseFaucet: Boolean = false,
    val isLoading: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val gotFreeXrd: Boolean = false,
    val error: UiMessage? = null,
    val hasAuthKey: Boolean = false,
    val interactionState: InteractionState? = null
) : UiState
