package com.babylon.wallet.android.presentation.settings.addconnection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.model.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import okio.ByteString.Companion.decodeHex
import rdx.works.peerdroid.helpers.Result
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.AddP2PClientUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsAddConnectionViewModel @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    private val addP2PClientUseCase: AddP2PClientUseCase,
    profileRepository: ProfileRepository
) : ViewModel() {

    private val loadingState = MutableStateFlow(false)

    val uiState: StateFlow<SettingsAddConnectionUiState> = combine(
        loadingState,
        profileRepository.connectionPassword
    ) { isLoading, connectionPassword ->
        SettingsAddConnectionUiState(
            isLoading = isLoading,
            hasAlreadyConnection = connectionPassword.isEmpty().not()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
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
            addP2PClientUseCase.invoke(
                displayName = currentConnectionDisplayName,
                connectionPassword = currentConnectionPassword,
                isWithoutProfile = true
            )
            loadingState.value = false
        }
    }

    private fun parseEncryptionKeyFromConnectionPassword(connectionPassword: String): ByteArray? {
        return try {
            connectionPassword.decodeHex().toByteArray()
        } catch (iae: IllegalArgumentException) {
            Timber.e("failed to parse encryption key from connection id: ${iae.localizedMessage}")
            null
        }
    }
}

data class SettingsAddConnectionUiState(
    val isLoading: Boolean = false,
    val hasAlreadyConnection: Boolean = false
)
