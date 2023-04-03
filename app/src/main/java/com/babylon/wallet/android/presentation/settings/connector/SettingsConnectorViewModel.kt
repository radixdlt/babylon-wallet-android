package com.babylon.wallet.android.presentation.settings.connector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.peerdroid.helpers.Result
import rdx.works.profile.domain.p2plink.AddP2PLinkUseCase
import rdx.works.profile.domain.p2plink.DeleteP2PLinkUseCase
import rdx.works.profile.domain.p2plink.GetP2PLinksUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsConnectorViewModel @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    private val peerdroidLink: PeerdroidLink,
    getP2PLinksUseCase: GetP2PLinksUseCase,
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
            getP2PLinksUseCase()
                .collect { p2pLinks ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            activeConnectors = p2pLinks.map { p2pLink ->
                                ActiveConnectorUiModel(
                                    id = p2pLink.id,
                                    name = p2pLink.displayName,
                                    connectionPassword = p2pLink.connectionPassword
                                )
                            }.toPersistentList()
                        )
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
                when (peerdroidLink.addConnection(encryptionKey)) {
                    is Result.Success -> {
                        saveConnectionPassword(
                            connectionPassword = currentConnectionPassword,
                            connectorDisplayName = state.value.editedConnectorDisplayName
                        )
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

    fun onDeleteConnectorClick(connectionPassword: String) {
        viewModelScope.launch {
            deleteP2PLinkUseCase(connectionPassword)
            peerdroidClient.deleteLink(connectionPassword)
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

    private fun saveConnectionPassword(
        connectionPassword: String,
        connectorDisplayName: String
    ) {
        viewModelScope.launch {
            addP2PLinkUseCase(
                displayName = connectorDisplayName,
                connectionPassword = connectionPassword
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
    val activeConnectors: ImmutableList<ActiveConnectorUiModel> = persistentListOf(),
    val editedConnectorDisplayName: String = "",
    val buttonEnabled: Boolean = false,
    val isScanningQr: Boolean = false,
    val mode: SettingsConnectorMode = SettingsConnectorMode.ShowDetails,
    val triggerCameraPermissionPrompt: Boolean = false,
)

data class ActiveConnectorUiModel(
    val id: String,
    val name: String,
    val connectionPassword: String // needed for deletion
)

enum class SettingsConnectorMode {
    LinkConnector, ShowDetails, ScanQr
}
