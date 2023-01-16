package com.babylon.wallet.android.presentation.settings.editgateway

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.presentation.common.InfoMessageType
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.utils.encodeUtf8
import com.babylon.wallet.android.utils.isValidUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.repository.NetworkRepository
import javax.inject.Inject

@HiltViewModel
class SettingsEditGatewayViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val networkInfoRepository: NetworkInfoRepository,
) : ViewModel(), OneOffEventHandler<SettingsEditGatewayEvent> by OneOffEventHandlerImpl() {

    var state by mutableStateOf(SettingsUiState())
        private set

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            networkRepository.networkAndGateway.collect { networkAndGateway ->
                state = state.copy(
                    currentNetworkAndGateway = networkAndGateway,
                    newUrl = networkAndGateway.gatewayAPIEndpointURL
                )
            }
        }
    }

    fun onNewUrlChanged(newUrl: String) {
        state = state.copy(
            newUrlValid = newUrl != state.currentNetworkAndGateway?.gatewayAPIEndpointURL && newUrl.isValidUrl(),
            newUrl = newUrl
        )
    }

    fun onMessageShown() {
        state = state.copy(uiMessage = null)
    }

    fun onSwitchToClick() {
        viewModelScope.launch {
            val newGatewayInfo = networkInfoRepository.getNetworkInfo(state.newUrl)
            newGatewayInfo.onValue { networkName ->
                state = state.copy(newNetworkName = networkName)
                if (networkRepository.hasAccountOnNetwork(state.newUrl, networkName)) {
                    networkRepository.setNetworkAndGateway(state.newUrl, networkName)
                    state = state.copy(uiMessage = UiMessage.InfoMessage(type = InfoMessageType.GatewayUpdated))
                } else {
                    val urlEncoded = state.newUrl.encodeUtf8()
                    sendEvent(SettingsEditGatewayEvent.CreateProfileOnNetwork(urlEncoded, networkName))
                }
            }
            newGatewayInfo.onError {
                state = state.copy(uiMessage = UiMessage.InfoMessage(type = InfoMessageType.GatewayInvalid))
            }
        }
    }
}

@VisibleForTesting
internal sealed interface SettingsEditGatewayEvent : OneOffEvent {
    data class CreateProfileOnNetwork(val newUrl: String, val networkName: String) : SettingsEditGatewayEvent
}

data class SettingsUiState(
    val currentNetworkAndGateway: NetworkAndGateway? = null,
    val newUrl: String = "",
    val newNetworkName: String = "",
    val newUrlValid: Boolean = false,
    val uiMessage: UiMessage? = null,
)
