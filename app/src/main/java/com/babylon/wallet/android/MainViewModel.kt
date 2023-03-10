package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AppConstants
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.ConnectionStateChanged
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

    val state = combine(
        preferencesManager.showOnboarding,
        profileDataSource.profile,
        profileDataSource.isProfileCompatible
    ) { showOnboarding, profileSnapshot, profileCompatible ->
        MainUiState(
            loading = false,
            initialAppState = when {
                !profileCompatible -> AppNavigationState.IncompatibleProfile
                showOnboarding -> AppNavigationState.Onboarding
                profileSnapshot != null -> AppNavigationState.Wallet
                else -> AppNavigationState.CreateAccount
            }
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
        SharingStarted.WhileSubscribed(AppConstants.VM_STOP_TIMEOUT_MS),
        MainUiState()
    )

    private var currentConnectionPassword: String = ""
    private var incomingRequestsJob: Job? = null
    private var handlingRequestsJob: Job? = null

    init {
        profileDataSource.p2pClient
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
                        Timber.e("failed to connect to dapp")
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
                        // This message will be received
                        // when the user deletes the connection from connection settings screen.
                        // Therefore here we should not restart connection to dapp
                        // but to terminate the Peerdroid connection
                        if (message == ConnectionStateChanged.DELETE_CONNECTION) {
                            terminatePeerdroid()
                        }
                    } else if (message is IncomingRequest) {
                        handleIncomingRequest(message)
                    }
                }
        }
    }

    private fun handleIncomingRequest(request: IncomingRequest) {
        viewModelScope.launch {
            val verificationResult = verifyDappUseCase(request)
            verificationResult.onValue { verified ->
                if (verified) {
                    when (val result = authorizeSpecifiedPersonaUseCase(request)) {
                        is com.babylon.wallet.android.domain.common.Result.Error -> {
                            incomingRequestRepository.add(request)
                        }
                        is com.babylon.wallet.android.domain.common.Result.Success -> {
                            sendEvent(MainEvent.HandledUsePersonaAuthRequest(result.data))
                        }
                    }
                }
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
            peerdroidClient.close(shouldCloseConnectionToSignalingServer = true)
        }
    }

    fun deleteProfile() {
        viewModelScope.launch {
            profileDataSource.clear()
            preferencesManager.clear()
            peerdroidClient.close(shouldCloseConnectionToSignalingServer = true)
        }
    }

    companion object {
        private const val REQUEST_HANDLING_DELAY = 500L
    }
}

sealed class MainEvent : OneOffEvent {
    data class IncomingRequestEvent(val request: IncomingRequest) : MainEvent()
    data class HandledUsePersonaAuthRequest(val dAppName: String) : MainEvent()
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
