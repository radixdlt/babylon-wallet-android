package com.babylon.wallet.android.presentation.settings.connector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.peerdroid.helpers.Result
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.domain.AddP2PLinkUseCase
import rdx.works.profile.domain.DeleteP2PLinkUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsConnectorViewModel @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    profileDataSource: ProfileDataSource,
    private val addP2PLinkUseCase: AddP2PLinkUseCase,
    private val deleteP2PLinkUseCase: DeleteP2PLinkUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var currentConnectionPassword: String = ""

    private val args = SettingsConnectorScreenArgs(savedStateHandle)
    private val _state: MutableStateFlow<SettingsConnectorUiState> =
        MutableStateFlow(
            SettingsConnectorUiState(
                mode = if (args.scanQr) {
                    SettingsConnectorMode.ScanQr
                } else {
                    SettingsConnectorMode.ShowDetails
                },
                triggerCameraPermissionPrompt = args.scanQr
            )
        )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileDataSource.p2pLink.collect { p2pLink ->
                if (p2pLink != null) { // if we already have an active connector
                    // we need a reference of the connectionPassword
                    // so we can pass it in the onDeleteConnectorClick
                    currentConnectionPassword = p2pLink.connectionPassword
                }
                _state.update {
                    it.copy(isLoading = false, connectorName = p2pLink?.displayName)
                }
            }
        }
    }

    fun onLinkNewConnectorClick() {
        if (currentConnectionPassword.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val encryptionKey = parseEncryptionKeyFromConnectionPassword(
                connectionPassword = currentConnectionPassword
            )

            if (encryptionKey != null) {
                when (peerdroidClient.addConnection(encryptionKey)) {
                    is Result.Success -> {
                        saveConnectionPassword(connectorDisplayName = state.value.editedConnectorDisplayName)
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(isLoading = false)
                        }
                        Timber.d("Failed to connect to remote peer.")
                    }
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onConnectorDisplayNameChanged(name: String) {
        val displayNameChanged = name.trim()
        _state.update {
            it.copy(
                editedConnectorDisplayName = displayNameChanged,
                buttonEnabled = displayNameChanged.isNotEmpty()
            )
        }
    }

    fun onDeleteConnectorClick() {
        viewModelScope.launch {
            deleteP2PLinkUseCase(currentConnectionPassword)
            peerdroidClient.close(
                shouldCloseConnectionToSignalingServer = true,
                isDeleteConnectionEvent = true
            )
            _state.update { it.copy(connectorName = null) }
        }
    }

    fun onConnectionPasswordDecoded(connectionPassword: String) {
        _state.update { it.copy(mode = SettingsConnectorMode.LinkConnector) }
        currentConnectionPassword = connectionPassword
    }

    fun cancelQrScan() {
        _state.update { it.copy(mode = SettingsConnectorMode.ShowDetails) }
    }

    fun linkConnector() {
        _state.update { it.copy(mode = SettingsConnectorMode.ScanQr) }
    }

    private fun saveConnectionPassword(connectorDisplayName: String) {
        viewModelScope.launch {
            peerdroidClient.close(shouldCloseConnectionToSignalingServer = true)
            addP2PLinkUseCase(
                displayName = connectorDisplayName,
                connectionPassword = currentConnectionPassword
            )
            _state.update {
                it.copy(
                    isLoading = false,
                    mode = SettingsConnectorMode.ShowDetails,
                    editedConnectorDisplayName = "",
                    buttonEnabled = false
                )
            }
        }
    }
}

data class SettingsConnectorUiState(
    val isLoading: Boolean = true,
    val connectorName: String? = null,
    val editedConnectorDisplayName: String = "",
    val buttonEnabled: Boolean = false,
    val isScanningQr: Boolean = false,
    val mode: SettingsConnectorMode = SettingsConnectorMode.ShowDetails,
    val triggerCameraPermissionPrompt: Boolean = false,
)

enum class SettingsConnectorMode {
    LinkConnector, ShowDetails, ScanQr
}
