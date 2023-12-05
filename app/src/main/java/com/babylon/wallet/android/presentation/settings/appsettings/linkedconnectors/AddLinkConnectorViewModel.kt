package com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.HexCoded32Bytes
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.profile.domain.p2plink.AddP2PLinkUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddLinkConnectorViewModel @Inject constructor(
    private val peerdroidLink: PeerdroidLink,
    private val addP2PLinkUseCase: AddP2PLinkUseCase
) : StateViewModel<AddLinkConnectorUiState>() {

    private var currentConnectionPassword: HexCoded32Bytes? = null

    override fun initialState() = AddLinkConnectorUiState.init

    fun onQrCodeScanned(connectionPassword: String) {
        runCatching {
            currentConnectionPassword = HexCoded32Bytes(connectionPassword)
        }.onSuccess {
            _state.update { state ->
                state.copy(showContent = AddLinkConnectorUiState.ShowContent.NameLinkConnector)
            }
        }.onFailure {
            _state.update { state ->
                state.copy(invalidConnectionPassword = true)
            }
        }
    }

    fun onConnectorDisplayNameChanged(name: String) {
        _state.update {
            it.copy(
                connectorDisplayName = name,
                isContinueButtonEnabled = name.trim().isNotEmpty()
            )
        }
    }

    fun onContinueClick() {
        viewModelScope.launch {
            _state.update {
                it.copy(isAddingNewLinkConnectorInProgress = true)
            }
            val encryptionKey = currentConnectionPassword?.let {
                parseEncryptionKeyFromConnectionPassword(connectionPassword = it.value)
            }
            if (encryptionKey != null) {
                val connectionPassword = requireNotNull(currentConnectionPassword)
                peerdroidLink.addConnection(encryptionKey)
                    .onSuccess {
                        addP2PLinkUseCase(
                            displayName = state.value.connectorDisplayName,
                            connectionPassword = connectionPassword.value
                        )
                    }
                    .onFailure {
                        Timber.d("Failed to connect to remote peer.")
                    }
            }
            _state.value = AddLinkConnectorUiState.init
        }
    }

    fun onCloseClick() {
        currentConnectionPassword = null
        _state.value = AddLinkConnectorUiState.init
    }

    fun onInvalidConnectionPasswordShown() {
        _state.update { it.copy(invalidConnectionPassword = false) }
    }
}

data class AddLinkConnectorUiState(
    val isAddingNewLinkConnectorInProgress: Boolean,
    val showContent: ShowContent,
    val isContinueButtonEnabled: Boolean,
    val connectorDisplayName: String,
    val invalidConnectionPassword: Boolean = false
) : UiState {

    enum class ShowContent {
        ScanQrCode, NameLinkConnector
    }

    companion object {
        val init = AddLinkConnectorUiState(
            isAddingNewLinkConnectorInProgress = false,
            showContent = ShowContent.ScanQrCode,
            isContinueButtonEnabled = false,
            connectorDisplayName = ""
        )
    }
}
