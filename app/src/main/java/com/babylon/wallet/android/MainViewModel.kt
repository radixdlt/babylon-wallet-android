package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.ConnectionStateChanged
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.peerdroid.helpers.Result
import rdx.works.profile.data.repository.ProfileDataSource
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesManager: PreferencesManager,
    profileDataSource: ProfileDataSource,
    private val peerdroidClient: PeerdroidClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val dappMetadataRepository: DappMetadataRepository
) : ViewModel(), OneOffEventHandler<MainEvent> by OneOffEventHandlerImpl() {

    val state = combine(
        preferencesManager.showOnboarding,
        profileDataSource.profile
    ) { showOnboarding, profileSnapshot ->
        MainUiState(
            loading = false,
            hasProfile = profileSnapshot != null,
            showOnboarding = showOnboarding
        )
    }.onStart {
        // this will also ensure that it won't execute when the viewmodel is initialized the first time
        if (currentConnectionPassword.isNotBlank()) {
            connectToDapp(currentConnectionPassword)
        }
    }.onCompletion {
        terminatePeerdroid()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        MainUiState()
    )

    private var currentConnectionPassword: String = ""
    private var observeP2PJob: Job? = null
    private var incomingRequestsJob: Job? = null
    private var handlingRequestsJob: Job? = null

    init {
        observeP2PJob = profileDataSource.p2pClient
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
        handlingRequestsJob = viewModelScope.launch {
            incomingRequestRepository.currentRequestToHandle.collect { request ->
                delay(REQUEST_HANDLING_DELAY)
                sendEvent(MainEvent.IncomingRequestEvent(request))
            }
        }

        incomingRequestsJob = viewModelScope.launch {
            peerdroidClient
                .listenForIncomingRequests()
                .cancellable()
                .collect { message ->
                    if (message is ConnectionStateChanged) {
                        if (message == ConnectionStateChanged.CLOSING || message == ConnectionStateChanged.CLOSE) {
                            restartConnectionToDapp()
                        }
                    } else if (message is IncomingRequest) {
                        handleIncomingRequest(message)
                    }
                }
        }
    }

    private fun handleIncomingRequest(request: IncomingRequest) {
        viewModelScope.launch {
            val result =
                dappMetadataRepository.verifyDappSimple(request.metadata.origin, request.metadata.dAppDefinitionAddress)
            if (result is com.babylon.wallet.android.domain.common.Result.Success && result.data) {
                when (request) {
                    is IncomingRequest.AuthorizedRequest -> {
                        // TODO auth request
                    }
                    is IncomingRequest.TransactionRequest -> {
                        incomingRequestRepository.add(request)
                    }
                    is IncomingRequest.UnauthorizedRequest -> {
                        // TODO no auth request
                    }
                }
            } else {
                // TODO dApp verification failed
            }
        }
    }

    private fun restartConnectionToDapp() {
        viewModelScope.launch {
            incomingRequestsJob?.cancel()
            handlingRequestsJob?.cancel()
            peerdroidClient.close()
            connectToDapp(currentConnectionPassword)
        }
    }

    private fun terminatePeerdroid() {
        viewModelScope.launch {
            incomingRequestsJob?.cancel()
            handlingRequestsJob?.cancel()
            observeP2PJob?.cancel()
            peerdroidClient.close(shouldCloseConnectionToSignalingServer = true)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
        private const val REQUEST_HANDLING_DELAY = 500L
    }
}

sealed class MainEvent : OneOffEvent {
    data class IncomingRequestEvent(val request: IncomingRequest) : MainEvent()
}

data class MainUiState(
    val loading: Boolean = true,
    val hasProfile: Boolean = false,
    val showOnboarding: Boolean = false,
)
