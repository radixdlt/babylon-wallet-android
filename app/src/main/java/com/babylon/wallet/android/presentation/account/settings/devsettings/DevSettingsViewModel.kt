package com.babylon.wallet.android.presentation.account.settings.devsettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.babylon.wallet.android.domain.usecases.signing.ROLAClient
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.asProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.hasAuthSigning
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.account.AddAuthSigningFactorInstanceUseCase
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class DevSettingsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val rolaClient: ROLAClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val addAuthSigningFactorInstanceUseCase: AddAuthSigningFactorInstanceUseCase,
    private val transactionStatusClient: TransactionStatusClient,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<DevSettingsUiState>() {

    private val args = DevSettingsArgs(savedStateHandle)
    private var createAndUploadAuthKeyJob: Job? = null

    override fun initialState(): DevSettingsUiState = DevSettingsUiState(
        accountAddress = args.address,
    )

    init {
        loadAccount()
    }

    private fun loadAccount() {
        viewModelScope.launch {
            getProfileUseCase.flow.mapNotNull { profile ->
                profile.activeAccountsOnCurrentNetwork.firstOrNull { it.address == args.address }
            }.collect { account ->
                _state.update { state ->
                    state.copy(
                        account = account,
                        hasAuthKey = account.hasAuthSigning
                    )
                }
            }
        }
    }

    fun onCreateAndUploadAuthKey() {
        createAndUploadAuthKeyJob = viewModelScope.launch {
            state.value.account?.let { account ->
                val entity = account.asProfileEntity()
                _state.update { it.copy(isLoading = true) }
                rolaClient.generateAuthSigningFactorInstance(entity).onSuccess { authSigningFactorInstance ->
                    val manifest = rolaClient
                        .createAuthKeyManifest(entity, authSigningFactorInstance)
                        .getOrElse {
                            _state.update { state ->
                                state.copy(isLoading = false)
                            }
                            return@launch
                        }
                    Timber.d("Approving: \n$manifest")
                    val interactionId = UUID.randomUUID().toString()
                    incomingRequestRepository.add(
                        manifest.prepareInternalTransactionRequest(
                            requestId = interactionId,
                            blockUntilCompleted = true,
                            transactionType = TransactionType.CreateRolaKey(authSigningFactorInstance)
                        )
                    )
                    _state.update { it.copy(isLoading = false) }
                    listenForRolaKeyUploadTransactionResult(interactionId.toString())
                }.onFailure {
                    if (it is ProfileException.SecureStorageAccess) {
                        appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                    }
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
                            val account = requireNotNull(state.value.account).asProfileEntity()
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
    val account: Account? = null,
    val accountAddress: AccountAddress,
    val isLoading: Boolean = false,
    val hasAuthKey: Boolean = false
) : UiState
