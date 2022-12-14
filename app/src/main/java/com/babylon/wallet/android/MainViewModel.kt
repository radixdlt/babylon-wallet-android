package com.babylon.wallet.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.DataStoreManager
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.model.IncomingRequest
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.peerdroid.helpers.Result
import rdx.works.profile.data.repository.ProfileRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesManager: DataStoreManager,
    profileRepository: ProfileRepository,
    private val peerdroidClient: PeerdroidClient,
) : ViewModel() {

    val state = preferencesManager.showOnboarding.map { showOnboarding ->
        MainUiState(
            loading = false,
            hasProfile = profileRepository.readProfileSnapshot() != null,
            showOnboarding = showOnboarding
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        MainUiState()
    )

    var incomingRequest by mutableStateOf<IncomingRequest>(IncomingRequest.Empty)
        private set

    init {
        // TODO don't start this if profile doesn't exist
        profileRepository.connectionPassword
            .map { connectionPassword ->
                if (connectionPassword.isNotEmpty()) {
                    Timber.d("found connection password")
                    connectToDapp(connectionPassword)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun connectToDapp(connectionPassword: String) {
        val encryptionKey = parseEncryptionKeyFromConnectionPassword(
            connectionPassword = connectionPassword
        )
        if (encryptionKey != null) {
            viewModelScope.launch {
                when (peerdroidClient.connectToRemotePeerWithEncryptionKey(encryptionKey)) {
                    is Result.Success -> {
                        Timber.d("connected to dapp")
                        listenForIncomingDappRequests()
                    }
                    is Result.Error -> {
                        Timber.d("failed to connect to dapp")
                    }
                }
            }
        }
    }

    private fun listenForIncomingDappRequests() {
        viewModelScope.launch {
            peerdroidClient
                .listenForIncomingRequests()
                .collect { request ->
                    incomingRequest = request
                    Timber.d("=========> request in main is $request")
                }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

data class MainUiState(
    val loading: Boolean = true,
    val hasProfile: Boolean = false,
    val showOnboarding: Boolean = false
)
