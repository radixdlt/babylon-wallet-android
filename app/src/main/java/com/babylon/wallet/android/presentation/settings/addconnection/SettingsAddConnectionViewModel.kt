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
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okio.ByteString.Companion.decodeHex
import rdx.works.peerdroid.helpers.Result
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsAddConnectionViewModel @Inject constructor(
    private val peerdroidClient: PeerdroidClient
) : ViewModel() {

    var state by mutableStateOf(SettingsAddConnectionUiState())
        private set

    // TODO we later store this in the profile
    private var currentConnectionPassword: String = ""
    private var listenIncomingMessagesJob: Job? = null

    init {
        state = state.copy(
            isLoading = false,
            isConnectionOpen = peerdroidClient.isAlreadyOpen
        )
    }

    fun onConnectionClick(connectionPassword: String) {
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
                        listenForIncomingRequests()
                        state.copy(isConnectionOpen = true)
                    }
                    is Result.Error -> {
                        retryConnection()
                        state.copy(isConnectionOpen = false)
                    }
                }
            }
            state = state.copy(isLoading = false)
        }
    }

    private fun listenForIncomingRequests() {
        listenIncomingMessagesJob = viewModelScope.launch {
            peerdroidClient.listenForEvents()
                .cancellable()
                .onEach { state ->
                    Timber.d("state: $state")
                    this@SettingsAddConnectionViewModel.state = this@SettingsAddConnectionViewModel.state.copy(
                        isLoading = state == ConnectionState.CONNECTING,
                        isConnectionOpen = state == ConnectionState.OPEN
                    )
                    if (state == ConnectionState.CLOSE) {
                        retryConnection()
                    }
                }.collect()
        }
    }

    private suspend fun retryConnection() {
        Timber.d("retry connection")
        peerdroidClient.close()
        onConnectionClick(currentConnectionPassword)
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
