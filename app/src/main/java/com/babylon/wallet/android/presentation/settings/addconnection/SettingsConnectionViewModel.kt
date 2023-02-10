package com.babylon.wallet.android.presentation.settings.addconnection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.ConnectionStateChanged
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.peerdroid.helpers.Result
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.domain.AddP2PClientUseCase
import rdx.works.profile.domain.DeleteP2PClientUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsConnectionViewModel @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    profileDataSource: ProfileDataSource,
    private val addP2PClientUseCase: AddP2PClientUseCase,
    private val deleteP2PClientUseCase: DeleteP2PClientUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var currentConnectionPassword: String = ""
    private var currentConnectionDisplayName: String = ""
    private var listenIncomingMessagesJob: Job? = null

    private val args = SettingsConnectionScreenArgs(savedStateHandle)
    private val _state: MutableStateFlow<SettingsConnectionUiState> =
        MutableStateFlow(
            SettingsConnectionUiState(
                mode = if (args.scanQr) {
                    SettingsConnectionMode.ScanQr
                } else {
                    SettingsConnectionMode.ShowDetails
                },
                triggerCameraPermissionPrompt = args.scanQr
            )
        )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileDataSource.p2pClient.collect { p2pClient ->
                if (p2pClient != null) { // if we already have an active connection
                    // we need a reference of the connectionPassword
                    // so we can pass it in the onDeleteConnectionClick
                    currentConnectionPassword = p2pClient.connectionPassword
                }
                _state.update {
                    it.copy(isLoading = false, connectionName = p2pClient?.displayName)
                }
            }
        }
    }

    fun onConnectionClick() {
        if (listenIncomingMessagesJob?.isActive == true) {
            listenIncomingMessagesJob?.cancel()
        }
        if (currentConnectionPassword.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val encryptionKey = parseEncryptionKeyFromConnectionPassword(
                connectionPassword = currentConnectionPassword
            )

            if (encryptionKey != null) {
                when (peerdroidClient.connectToRemotePeerWithEncryptionKey(encryptionKey)) {
                    is Result.Success -> { // we have a data channel which is already open!
                        currentConnectionDisplayName = state.value.editedConnectionDisplayName
                        waitUntilConnectionIsTerminated()
                    }
                    is Result.Error -> {
                        Timber.d("Failed to connect to remote peer.")
                    }
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onConnectionDisplayNameChanged(name: String) {
        val displayNameChanged = name.trim()
        _state.update {
            it.copy(
                editedConnectionDisplayName = displayNameChanged,
                buttonEnabled = displayNameChanged.isNotEmpty()
            )
        }
    }

    fun onDeleteConnectionClick() {
        viewModelScope.launch {
            deleteP2PClientUseCase(currentConnectionPassword)
            peerdroidClient.close(
                shouldCloseConnectionToSignalingServer = true,
                isDeleteConnectionEvent = true
            )
            _state.update { it.copy(connectionName = null) }
        }
    }

    fun onConnectionPasswordDecoded(connectionPassword: String) {
        _state.update { it.copy(mode = SettingsConnectionMode.AddConnection) }
        currentConnectionPassword = connectionPassword
    }

    fun cancelQrScan() {
        _state.update { it.copy(mode = SettingsConnectionMode.ShowDetails) }
    }

    fun addConnection() {
        _state.update { it.copy(mode = SettingsConnectionMode.ScanQr) }
    }

    // This function is triggered when the data channel is open
    // and finishes its job when the channel is closed.
    // Because that means we successfully established a connection with connector extension
    // and the connection password has been passed to the dapp.
    private fun waitUntilConnectionIsTerminated() {
        listenIncomingMessagesJob = viewModelScope.launch {
            peerdroidClient
                .listenForStateEvents()
                .cancellable()
                .collect { connectionState ->
                    if (connectionState == ConnectionStateChanged.CLOSE ||
                        connectionState == ConnectionStateChanged.CLOSING
                    ) {
                        saveConnectionPassword()
                    }
                }
        }
    }

    private fun saveConnectionPassword() {
        viewModelScope.launch {
            listenIncomingMessagesJob?.cancel()
            peerdroidClient.close()
            addP2PClientUseCase(
                displayName = currentConnectionDisplayName,
                connectionPassword = currentConnectionPassword
            )
            _state.update {
                it.copy(
                    isLoading = false,
                    mode = SettingsConnectionMode.ShowDetails,
                    editedConnectionDisplayName = "",
                    buttonEnabled = false
                )
            }
        }
    }
}

data class SettingsConnectionUiState(
    val isLoading: Boolean = true,
    val connectionName: String? = null,
    val editedConnectionDisplayName: String = "",
    val buttonEnabled: Boolean = false,
    val isScanningQr: Boolean = false,
    val mode: SettingsConnectionMode = SettingsConnectionMode.ShowDetails,
    val triggerCameraPermissionPrompt: Boolean = false,
)

enum class SettingsConnectionMode {
    AddConnection, ShowDetails, ScanQr
}
