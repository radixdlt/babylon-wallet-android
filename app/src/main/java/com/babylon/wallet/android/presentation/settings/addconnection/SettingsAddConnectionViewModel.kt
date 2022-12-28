package com.babylon.wallet.android.presentation.settings.addconnection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.model.ConnectionState
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.peerdroid.helpers.Result
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.AddP2PClientUseCase
import rdx.works.profile.domain.DeleteP2PClientUseCase
import timber.log.Timber
import javax.inject.Inject

private const val FIVE_SECONDS_STOP_TIMEOUT = 5_000L

@HiltViewModel
class SettingsAddConnectionViewModel @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    profileRepository: ProfileRepository,
    private val addP2PClientUseCase: AddP2PClientUseCase,
    private val deleteP2PClientUseCase: DeleteP2PClientUseCase
) : ViewModel() {

    private val loadingState = MutableStateFlow(false)

    val uiState: StateFlow<SettingsAddConnectionUiState> = combine(
        loadingState,
        profileRepository.p2pClient
    ) { isLoading, p2pClient ->
        if (p2pClient != null) { // if we already have an active connection
            // we need a reference of the connectionPassword
            // so we can pass it in the onDeleteConnectionClick
            currentConnectionPassword = p2pClient.connectionPassword
        }
        SettingsAddConnectionUiState(
            isLoading = isLoading,
            connectionName = p2pClient?.displayName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(FIVE_SECONDS_STOP_TIMEOUT),
        initialValue = SettingsAddConnectionUiState()
    )

    private var currentConnectionPassword: String = ""
    private var currentConnectionDisplayName: String = ""
    private var listenIncomingMessagesJob: Job? = null

    fun onConnectionClick(
        connectionPassword: String,
        connectionDisplayName: String
    ) {
        if (listenIncomingMessagesJob?.isActive == true) {
            listenIncomingMessagesJob?.cancel()
        }

        viewModelScope.launch {
            loadingState.value = true

            val encryptionKey = parseEncryptionKeyFromConnectionPassword(
                connectionPassword = connectionPassword
            )

            if (encryptionKey != null) {
                when (peerdroidClient.connectToRemotePeerWithEncryptionKey(encryptionKey)) {
                    is Result.Success -> { // we have a data channel which is already open!
                        currentConnectionPassword = connectionPassword
                        currentConnectionDisplayName = connectionDisplayName
                        waitUntilConnectionIsTerminated()
                    }
                    is Result.Error -> {
                        Timber.d("Failed to connect to remote peer.")
                    }
                }
            } else {
                loadingState.value = false
            }
        }
    }

    fun onDeleteConnectionClick() {
        viewModelScope.launch {
            deleteP2PClientUseCase(currentConnectionPassword)
            peerdroidClient.close()
        }
    }

    // This function is triggered when the data channel is open
    // and finishes its job when the channel is closed.
    // Because that means we successfully established a connection with connector extension
    // and the connection password has been passed to the dapp.
    private fun waitUntilConnectionIsTerminated() {
        listenIncomingMessagesJob = viewModelScope.launch {
            peerdroidClient
                .listenForStateEvents()
                .collect { connectionState ->
                    if (connectionState == ConnectionState.CLOSE || connectionState == ConnectionState.CLOSING) {
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
            loadingState.value = false
        }
    }
}

data class SettingsAddConnectionUiState(
    val isLoading: Boolean = false,
    val connectionName: String? = null
)
