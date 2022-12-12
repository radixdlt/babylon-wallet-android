package com.babylon.wallet.android.presentation.settings.addconnection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.model.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.ByteString.Companion.decodeHex
import rdx.works.peerdroid.helpers.Result
import rdx.works.profile.domain.AddP2PClientUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsAddConnectionViewModel @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    private val addP2PClientUseCase: AddP2PClientUseCase
) : ViewModel() {

    var state by mutableStateOf(SettingsAddConnectionUiState())
        private set

    private var currentConnectionPassword: String = ""
    private var currentConnectionDisplayName: String = ""
    private var listenIncomingMessagesJob: Job? = null

    init {
        state = state.copy(
            isLoading = false,
            isConnectionOpen = peerdroidClient.isAlreadyOpen
        )
    }

    fun onConnectionClick(
        connectionPassword: String,
        connectionDisplayName: String
    ) {
        if (listenIncomingMessagesJob?.isActive == true) {
            listenIncomingMessagesJob?.cancel()
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true)

            val encryptionKey = parseEncryptionKeyFromConnectionPassword(
                connectionPassword = connectionPassword
            )

            if (encryptionKey != null) {
                val result = peerdroidClient.connectToRemotePeerWithEncryptionKey(encryptionKey)
                state = when (result) {
                    is Result.Success -> {
                        currentConnectionPassword = connectionPassword
                        currentConnectionDisplayName = connectionDisplayName
                        waitUntilConnectionIsEstablished()
                        state.copy(isConnectionOpen = true)
                    }
                    is Result.Error -> {
                        state.copy(isConnectionOpen = false)
                    }
                }
            }
            state = state.copy(isLoading = false)
        }
    }

    // This function is triggered when the data channel is open
    // and finishes its job when the channel is closed.
    // Because that means we successfully established a connection with connector extension
    // and the connection password has been passed to the dapp.
    private fun waitUntilConnectionIsEstablished() {
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
    val isConnectionOpen: Boolean = false
)
