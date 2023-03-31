package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AppConstants
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.domain.usecases.AuthorizeSpecifiedPersonaUseCase
import com.babylon.wallet.android.domain.usecases.VerifyDappUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
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
    private val preferencesManager: PreferencesManager,
    private val profileDataSource: ProfileDataSource,
    private val peerdroidClient: PeerdroidClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val authorizeSpecifiedPersonaUseCase: AuthorizeSpecifiedPersonaUseCase,
    private val verifyDappUseCase: VerifyDappUseCase
) : ViewModel(), OneOffEventHandler<MainEvent> by OneOffEventHandlerImpl() {

    private var observeP2PLinksJob: Job? = null
    private var incomingRequestsJob: Job? = null
    private var handlingCurrentRequestJob: Job? = null
    private var processingRequestJob: Job? = null

    val state = combine(
        preferencesManager.showOnboarding,
        profileDataSource.profileState
    ) { showOnboarding, profileState ->
        MainUiState(
            loading = false,
            initialAppState = when {
                profileState.isFailure -> AppNavigationState.IncompatibleProfile
                showOnboarding -> AppNavigationState.Onboarding
                profileState.getOrNull() != null -> AppNavigationState.Wallet
                else -> AppNavigationState.CreateAccount
            }
        )
    }.onStart {
        Timber.d("start observing for p2p links")
        observeForP2PLinks()
    }.onCompletion {
        terminatePeerdroid()
        incomingRequestRepository.removeAll()
        observeP2PLinksJob?.cancel()
        Timber.d("terminate main view model state")
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(AppConstants.VM_STOP_TIMEOUT_MS),
        MainUiState()
    )

    fun incomingRequestHandled(requestId: String) {
        viewModelScope.launch {
            incomingRequestRepository.requestHandled(requestId)
        }
    }

    fun deleteProfile() {
        viewModelScope.launch {
            profileDataSource.clear()
            preferencesManager.clear()
            peerdroidClient.terminate()
        }
    }

    private fun observeForP2PLinks() {
        observeP2PLinksJob = profileDataSource.p2pLinks
            .map { p2pLinks ->
                Timber.d("found ${p2pLinks.size} p2p links")
                p2pLinks.forEach { p2PLink ->
                    establishLinkConnection(connectionPassword = p2PLink.connectionPassword)
                }
                p2pLinks
            }
            .launchIn(viewModelScope)
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
            }
        }
    }

    private fun listenForIncomingDappRequests() {
        handlingCurrentRequestJob = viewModelScope.launch {
            incomingRequestRepository.currentRequestToHandle.collect { request ->
                delay(REQUEST_HANDLING_DELAY)
                sendEvent(MainEvent.IncomingRequestEvent(request))
            }
        }

        incomingRequestsJob = viewModelScope.launch {
            peerdroidClient
                .listenForIncomingRequests()
                .filterIsInstance<IncomingRequest>()
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
            val verificationResult = verifyDappUseCase(request)
            verificationResult.onValue { verified ->
                if (verified) {
                    when (val dAppData = authorizeSpecifiedPersonaUseCase(request)) {
                        is com.babylon.wallet.android.domain.common.Result.Error -> {
                            incomingRequestRepository.add(request)
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
        }
    }

    private fun terminatePeerdroid() {
        incomingRequestsJob?.cancel()
        handlingCurrentRequestJob?.cancel()
        handlingCurrentRequestJob = null
        processingRequestJob?.cancel()
        peerdroidClient.terminate()
        Timber.d("Peerdroid terminated")
    }

    companion object {
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
    val loading: Boolean = true,
    val initialAppState: AppNavigationState = AppNavigationState.Init
)

sealed interface AppNavigationState {
    object Onboarding : AppNavigationState
    object Wallet : AppNavigationState
    object CreateAccount : AppNavigationState
    object IncompatibleProfile : AppNavigationState
    object Init : AppNavigationState
}
