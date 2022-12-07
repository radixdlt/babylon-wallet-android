package com.babylon.wallet.android.presentation.settings.editgateway

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.utils.isValidUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsEditGatewayViewModel @Inject constructor() : ViewModel() {

    var state by mutableStateOf(SettingsUiState())
        private set

    fun onNewUrlChanged(newUrl: String) {
        state = state.copy(
            newUrlValid = newUrl != state.currentNetworkData.networkEndpoint && newUrl.isValidUrl(),
            newUrl = newUrl
        )
    }

    fun onSwitchToClick() {
        Timber.d("Replace network")
    }
}

// TODO replace with real data from profile
data class TempNetworkData(
    val networkName: String = "Hammunet",
    val networkId: String = "34",
    val networkEndpoint: String = BuildConfig.GATEWAY_API_URL
)

data class SettingsUiState(
    val currentNetworkData: TempNetworkData = TempNetworkData(),
    val newUrl: String = "",
    val newUrlValid: Boolean = false
)
