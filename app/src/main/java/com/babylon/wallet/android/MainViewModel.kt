package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.ConnectionStateChanged
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
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
    preferencesManager: PreferencesManager,
    profileRepository: ProfileRepository,
    private val peerdroidClient: PeerdroidClient,
    private val incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<MainEvent> by OneOffEventHandlerImpl() {

    val state = combine(
        preferencesManager.showOnboarding,
        profileRepository.profileSnapshot
    ) { showOnboarding, profileSnapshot ->
        MainUiState(
            loading = false,
            hasProfile = profileSnapshot != null,
            showOnboarding = showOnboarding
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        MainUiState()
    )

    private var currentConnectionPassword: String = ""
    private var incomingRequestsJob: Job? = null

    init {
        profileRepository.p2pClient
            .map { p2pClient ->
                if (p2pClient != null) {
                    Timber.d("found connection password")
                    currentConnectionPassword = p2pClient.connectionPassword
                    connectToDapp(currentConnectionPassword)
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
        incomingRequestsJob = viewModelScope.launch {
            peerdroidClient
                .listenForIncomingRequests()
                .cancellable()
                .collect { message ->
                    if (message is ConnectionStateChanged) {
                        if (message == ConnectionStateChanged.CLOSING || message == ConnectionStateChanged.CLOSE) {
                            restartConnectionToDapp()
                        }
                    } else if (message is IncomingRequest && message != IncomingRequest.None) {
                        incomingRequestRepository.add(message)
                        sendEvent(MainEvent.IncomingRequestEvent(message))
                    }
                }
        }
    }

    private fun restartConnectionToDapp() {
        viewModelScope.launch {
            incomingRequestsJob?.cancel()
            peerdroidClient.close()
            connectToDapp(currentConnectionPassword)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

sealed class MainEvent : OneOffEvent {
    data class IncomingRequestEvent(val request: IncomingRequest) : MainEvent()
}

data class MainUiState(
    val loading: Boolean = true,
    val hasProfile: Boolean = false,
    val showOnboarding: Boolean = false
)
