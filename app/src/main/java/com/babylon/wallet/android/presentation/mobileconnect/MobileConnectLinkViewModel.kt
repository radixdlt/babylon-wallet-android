package com.babylon.wallet.android.presentation.mobileconnect

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.domain.model.Browser
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.RadixConnectMobile
import com.radixdlt.sargon.Url
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
@HiltViewModel
class MobileConnectLinkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val getDAppsUseCase: GetDAppsUseCase,
    private val radixConnectMobile: RadixConnectMobile
) : StateViewModel<MobileConnectLinkViewModel.State>(), OneOffEventHandler<MobileConnectLinkViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = MobileConnectArgs(savedStateHandle)
    override fun initialState(): State {
        return State()
    }

    init {
        observeAutoLink()
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
                if (!developerMode) {
                    _state.update {
                        it.copy(uiMessage = UiMessage.ErrorMessage(error), isLoading = false)
                    }

                    delay(Constants.SNACKBAR_SHOW_DURATION_MS)
                    sendEvent(Event.Close)
                } else if (_state.value.autoLink) {
                    linkWithDApp(autoLinked = true)
                }
            }

            if (_state.value.autoLink) {
                linkWithDApp(autoLinked = true)
            }
        }
    }

    private fun observeAutoLink() {
        viewModelScope.launch {
            preferencesManager.mobileConnectAutoLink.collect { autoLink ->
                _state.update {
                    it.copy(autoLink = autoLink)
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun linkWithDApp(autoLinked: Boolean) {
        _state.update {
            it.copy(isLinking = true)
        }

        if (autoLinked) {
            delay(AUTOLINK_DELAY)
        }

        runCatching {
            radixConnectMobile.handleLinkingRequest(request = args.request, devMode = _state.value.isInDevMode)
        }.onSuccess { callbackUrl ->
            sendEvent(
                Event.OpenUrl(
                    url = callbackUrl,
                    browser = Browser.fromBrowserName(args.request.browser)
                )
            )
        }.onFailure { error ->
            Timber.w(error)
            _state.update {
                it.copy(uiMessage = UiMessage.ErrorMessage(error), isLinking = false)
            }
            delay(Constants.SNACKBAR_SHOW_DURATION_MS)
            sendEvent(Event.Close)
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onAutoConfirmChange(autoConfirm: Boolean) {
        viewModelScope.launch {
            preferencesManager.autoLinkWithDapps(autoConfirm)
        }
    }

    fun onLinkWithDApp() {
        viewModelScope.launch {
            linkWithDApp(autoLinked = false)
        }
    }

    sealed class Event : OneOffEvent {
        data class OpenUrl(val url: Url, val browser: Browser) : Event()
        data object Close : Event()
    }

    data class State(
        val dApp: DApp? = null,
        val uiMessage: UiMessage? = null,
        val isLoading: Boolean = true,
        val linkDelaySeconds: Int = 0,
        val autoLink: Boolean = false,
        val isLinking: Boolean = false,
        val isInDevMode: Boolean = false
    ) : UiState

    companion object {
        private val AUTOLINK_DELAY = 2.seconds
    }
}
