package com.babylon.wallet.android.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.model.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okio.ByteString.Companion.decodeHex
import rdx.works.peerdroid.helpers.Result
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val peerdroidClient: PeerdroidClient
) : ViewModel() {

    var settingsState by mutableStateOf(SettingsUiState())
        private set

    init {
        settingsState = settingsState.copy(
            isLoading = false,
            isConnectionOpen = peerdroidClient.isAlreadyOpen
        )
    }

    fun onConnectionClick(connectionId: String) {
        viewModelScope.launch {
            settingsState = settingsState.copy(isLoading = true)
            val encryptionKey = parseEncryptionKeyFromConnectionId(
                connectionId = connectionId
            )
            if (encryptionKey != null) {
                val result = peerdroidClient.connectToRemoteClientWithEncryptionKey(encryptionKey)
                settingsState = when (result) {
                    is Result.Success -> {
                        listenForIncomingRequests()
                        settingsState.copy(isConnectionOpen = true)
                    }
                    is Result.Error -> {
                        settingsState.copy(isConnectionOpen = false)
                    }
                }
            }
            settingsState = settingsState.copy(isLoading = false)
        }
    }

    private fun listenForIncomingRequests() {
        viewModelScope.launch {
            peerdroidClient.listenForEvents().onEach { state ->
                settingsState = settingsState.copy(
                    isLoading = state == ConnectionState.CONNECTING,
                    isConnectionOpen = state == ConnectionState.OPEN
                )
            }.collect()
        }
    }

    private fun parseEncryptionKeyFromConnectionId(connectionId: String): ByteArray? {
        return try {
            connectionId.decodeHex().toByteArray()
        } catch (iae: IllegalArgumentException) {
            Timber.e("failed to parse encryption key from connection id: ${iae.localizedMessage}")
            null
        }
    }
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isConnectionOpen: Boolean = false
)
