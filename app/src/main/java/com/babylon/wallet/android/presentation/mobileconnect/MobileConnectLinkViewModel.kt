package com.babylon.wallet.android.presentation.mobileconnect

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.domain.model.IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.RadixConnectMobile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class MobileConnectLinkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val getDAppsUseCase: GetDAppsUseCase,
    private val radixConnectMobile: RadixConnectMobile,
    private val incomingRequestRepository: IncomingRequestRepository
) : StateViewModel<MobileConnectLinkViewModel.State>(), OneOffEventHandler<MobileConnectLinkViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = MobileConnectArgs(savedStateHandle)
    override fun initialState(): State {
        return State()
    }

    init {
        viewModelScope.launch {
            val developerMode = getProfileUseCase().appPreferences.security.isDeveloperModeEnabled
            _state.update {
                it.copy(
                    isLoading = true,
                    isInDevMode = developerMode
                )
            }

            wellKnownDAppDefinitionRepository.getWellKnownDappDefinitions(
                origin = args.request.origin.toString()
            ).then { dAppDefinitions ->
                val dAppDefinition = dAppDefinitions.dAppDefinitions.firstOrNull()
                if (dAppDefinition != null) {
                    getDAppsUseCase(definitionAddress = dAppDefinition.dAppDefinitionAddress, needMostRecentData = false)
                } else {
                    Result.failure(NullPointerException("No dApp definition found")) // TODO check that
                }
            }.onSuccess { dApp ->
                _state.update { it.copy(dApp = dApp, isLoading = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(uiMessage = if (!developerMode) UiMessage.ErrorMessage(error) else null, isLoading = false)
                }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onVerifyOrigin() = viewModelScope.launch {
        _state.update {
            it.copy(isVerifying = true)
        }

        runCatching {
            radixConnectMobile.requestOriginVerified(sessionId = args.request.sessionId)
            args.request.interaction.toDomainModel(
                remoteEntityId = RadixMobileConnectRemoteSession(id = args.request.sessionId.toString())
            ).getOrThrow()
        }.onSuccess { request ->
            incomingRequestRepository.add(request)
            sendEvent(Event.Close)
        }.onFailure { error ->
            Timber.w(error)
            _state.update {
                it.copy(uiMessage = UiMessage.ErrorMessage(error), isVerifying = false)
            }
        }
    }

    fun onDenyOrigin() = viewModelScope.launch {
        _state.update {
            it.copy(isVerifying = true)
        }

        runCatching {
            radixConnectMobile.requestOriginDenied(sessionId = args.request.sessionId)
        }.onSuccess {
            sendEvent(Event.Close)
        }.onFailure { error ->
            Timber.w(error)
            _state.update {
                it.copy(uiMessage = UiMessage.ErrorMessage(error), isVerifying = false)
            }
        }
    }

    sealed class Event : OneOffEvent {
        data object Close : Event()
    }

    data class State(
        val dApp: DApp? = null,
        val uiMessage: UiMessage? = null,
        val isLoading: Boolean = true,
        val isVerifying: Boolean = false,
        val isInDevMode: Boolean = false
    ) : UiState
}
