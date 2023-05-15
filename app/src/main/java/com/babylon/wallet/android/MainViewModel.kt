package com.babylon.wallet.android

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.domain.usecases.AuthorizeSpecifiedPersonaUseCase
import com.babylon.wallet.android.domain.usecases.VerifyDappUseCase
import com.babylon.wallet.android.domain.usecases.VerifyRequestVersionCompatibilityUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.peerdroid.helpers.Result
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.domain.GetProfileStateUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.p2pLinks
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
@HiltViewModel
class MainViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    private val peerdroidClient: PeerdroidClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val verifyRequestVersionCompatibilityUseCase: VerifyRequestVersionCompatibilityUseCase,
    private val authorizeSpecifiedPersonaUseCase: AuthorizeSpecifiedPersonaUseCase,
    private val verifyDappUseCase: VerifyDappUseCase,
    getProfileStateUseCase: GetProfileStateUseCase
) : StateViewModel<MainUiState>(), OneOffEventHandler<MainEvent> by OneOffEventHandlerImpl() {

    private var incomingRequestsJob: Job? = null
    private var handlingCurrentRequestJob: Job? = null
    private var processingRequestJob: Job? = null

    val observeP2PLinks = getProfileUseCase
        .p2pLinks
        .map { p2pLinks ->
            Timber.d("found ${p2pLinks.size} p2p links")
            p2pLinks.forEach { p2PLink ->
                establishLinkConnection(connectionPassword = p2PLink.connectionPassword)
            }
        }
        .onCompletion {
            Timber.d("Peerdroid is terminating")
            terminatePeerdroid()
        }
        .shareIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(PEERDROID_STOP_TIMEOUT) // TODO https://radixdlt.atlassian.net/browse/ABW-1421
        )

    init {
        viewModelScope.launch {
            getProfileStateUseCase()
                .collect { profileState ->
                    _state.update { MainUiState(initialAppState = AppState.from(profileState)) }
                }
        }
    }

    override fun initialState(): MainUiState {
        return MainUiState()
    }

    private suspend fun establishLinkConnection(connectionPassword: String) {
        val encryptionKey = parseEncryptionKeyFromConnectionPassword(
            connectionPassword = connectionPassword
        )
        if (encryptionKey != null) {
            when (val result = peerdroidClient.connect(encryptionKey = encryptionKey)) {
                is Result.Success -> {
                    Timber.d("Link connection established")
                    if (handlingCurrentRequestJob == null) {
                        Timber.d("Listen for incoming requests from dapps")
                        // We must run this only once
                        // otherwise for each new link connection
                        // we create a new job to collect messages from the same stream (messagesFromRemoteClients).
                        // I think this can be improved.
                        listenForIncomingDappRequests()
                    }
                }
                is Result.Error -> {
                    Timber.e("Failed to establish link connection: ${result.message}")
                }
                else -> {}
            }
        }
    }

    private fun listenForIncomingDappRequests() {
        handlingCurrentRequestJob = viewModelScope.launch {
            incomingRequestRepository.currentRequestToHandle.collect { request ->
                delay(REQUEST_HANDLING_DELAY)
                when (val dAppData = authorizeSpecifiedPersonaUseCase(request)) {
                    is com.babylon.wallet.android.domain.common.Result.Error -> {
                        sendEvent(MainEvent.IncomingRequestEvent(request))
                    }
                    is com.babylon.wallet.android.domain.common.Result.Success -> {
                        sendEvent(
                            MainEvent.HandledUsePersonaAuthRequest(
                                requestId = dAppData.data.requestId,
                                dAppName = dAppData.data.name
                            )
                        )
                    }
                }
            }
        }

        incomingRequestsJob = viewModelScope.launch {
            peerdroidClient
                .listenForIncomingRequests()
                .cancellable()
                .collect { incomingRequest ->
                    val remoteClient = incomingRequest.remoteClientId
                    val requestId = incomingRequest.id
                    Timber.d("📯 wallet received incoming request from remote client $remoteClient with id $requestId")
                    processIncomingRequest(incomingRequest)
                }
        }
    }

    private fun processIncomingRequest(request: IncomingRequest) {
        processingRequestJob = viewModelScope.launch {
            val requestVersionCompatibilityResult = verifyRequestVersionCompatibilityUseCase(request)
            requestVersionCompatibilityResult.onValue {
                val verificationResult = verifyDappUseCase(request)
                verificationResult.onValue { verified ->
                    if (verified) {
                        incomingRequestRepository.add(request)
                    }
                }
            }
        }
    }

    private fun terminatePeerdroid() {
        incomingRequestsJob?.cancel()
        handlingCurrentRequestJob?.cancel()
        handlingCurrentRequestJob = null
        processingRequestJob?.cancel()
        peerdroidClient.terminate()
        incomingRequestRepository.removeAll()
        Timber.d("Peerdroid terminated")
    }

    companion object {
        private val PEERDROID_STOP_TIMEOUT = 60.seconds
        private const val REQUEST_HANDLING_DELAY = 500L
    }
}

sealed class MainEvent : OneOffEvent {

    data class IncomingRequestEvent(val request: IncomingRequest) : MainEvent()

    data class HandledUsePersonaAuthRequest(
        val requestId: String,
        val dAppName: String
    ) : MainEvent()
}

data class MainUiState(
    val initialAppState: AppState = AppState.Loading
) : UiState

sealed interface AppState {
    object OnBoarding : AppState
    object NewProfile : AppState
    object Wallet : AppState
    object IncompatibleProfile : AppState
    object Loading : AppState

    companion object {
        fun from(profileState: ProfileState) = when (profileState) {
            is ProfileState.Incompatible -> IncompatibleProfile
            is ProfileState.Restored -> if (profileState.hasAnyAccount()) {
                Wallet
            } else {
                NewProfile
            }
            is ProfileState.None -> if (profileState.profileBackupExists) {
                OnBoarding
            } else {
                NewProfile
            }
            else -> NewProfile
        }
    }
}
