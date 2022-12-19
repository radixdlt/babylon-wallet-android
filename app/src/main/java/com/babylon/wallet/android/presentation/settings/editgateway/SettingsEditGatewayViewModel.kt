package com.babylon.wallet.android.presentation.settings.editgateway

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.presentation.common.InfoMessageType
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.utils.OneOffEventHandler
import com.babylon.wallet.android.utils.isValidUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.repository.ProfileRepository
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class SettingsEditGatewayViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val networkInfoRepository: NetworkInfoRepository
) : ViewModel() {

    var state by mutableStateOf(SettingsUiState())
        private set

    private val _oneOffEvent = OneOffEventHandler<OneOffEvent>()
    val oneOffEvent by _oneOffEvent

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.profileSnapshot.filterNotNull().collect { profileSnapshot ->
                profileSnapshot.appPreferences.networkAndGateway.let { networkAndGateway ->
                    state = state.copy(
                        currentNetworkAndGateway = networkAndGateway,
                        newUrl = networkAndGateway.gatewayAPIEndpointURL
                    )
                }
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
                if (profileRepository.hasAccountOnNetwork(state.newUrl, networkName)) {
                    profileRepository.setNetworkAndGateway(state.newUrl, networkName)
                    state = state.copy(uiMessage = UiMessage(messageType = InfoMessageType.GatewayUpdated))
                } else {
                    val urlEncoded = URLEncoder.encode(state.newUrl, StandardCharsets.UTF_8.toString())
                    _oneOffEvent.sendEvent(OneOffEvent.CreateProfileOnNetwork(urlEncoded, networkName))
                }
            }
            newGatewayInfo.onError {
                state = state.copy(uiMessage = UiMessage(messageType = InfoMessageType.GatewayInvalid))
            }
        }
    }

    sealed interface OneOffEvent {
        data class CreateProfileOnNetwork(val newUrl: String, val networkName: String) : OneOffEvent
    }
}

data class SettingsUiState(
    val currentNetworkAndGateway: NetworkAndGateway? = null,
    val newUrl: String = "",
    val newNetworkName: String = "",
    val newUrlValid: Boolean = false,
    val uiMessage: UiMessage? = null
)
