package com.babylon.wallet.android.presentation.settings.troubleshooting.manifest

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.init
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class SendTransactionManifestViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository
) : StateViewModel<SendTransactionManifestViewModel.State>() {

    private lateinit var networkId: NetworkId

    init {
        viewModelScope.launch {
            runCatching {
                requireNotNull(getProfileUseCase().currentNetwork?.id)
            }.onFailure {
                _state.update { state ->
                    state.copy(errorMessage = UiMessage.ErrorMessage(it))
                }
            }.onSuccess {
                networkId = it
            }
        }
    }

    override fun initialState(): State = State()

    fun onPreviewClick() {
        viewModelScope.launch {
            runCatching {
                val manifest = TransactionManifest.init(
                    instructionsString = state.value.manifest,
                    networkId = networkId
                )
                UnvalidatedManifestData.from(manifest).prepareInternalTransactionRequest()
            }.onFailure {
                _state.update { state ->
                    state.copy(errorMessage = UiMessage.ErrorMessage(it))
                }
            }.onSuccess {
                incomingRequestRepository.add(it)
            }
        }
    }

    fun onManifestChanged(value: String) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(manifest = value)
            }
        }
    }

    fun onClearClick() {
        _state.update { state ->
            state.copy(
                manifest = ""
            )
        }
    }

    fun onDismissErrorMessage() {
        _state.update { state ->
            state.copy(errorMessage = null)
        }
    }

    data class State(
        val manifest: String = "",
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val isManifestNotBlank: Boolean = manifest.isNotBlank()
    }
}