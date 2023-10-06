package com.babylon.wallet.android.presentation.settings.appsettings.gateways

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.encodeUtf8
import com.babylon.wallet.android.utils.isValidUrl
import com.babylon.wallet.android.utils.sanitizeAndValidateGatewayUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.AddGatewayUseCase
import rdx.works.profile.domain.gateway.ChangeGatewayUseCase
import rdx.works.profile.domain.gateway.DeleteGatewayUseCase
import rdx.works.profile.domain.gateways
import rdx.works.profile.domain.security
import javax.inject.Inject

@HiltViewModel
class GatewaysViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val changeGatewayUseCase: ChangeGatewayUseCase,
    private val addGatewayUseCase: AddGatewayUseCase,
    private val deleteGatewayUseCase: DeleteGatewayUseCase,
    private val networkInfoRepository: NetworkInfoRepository,
) : StateViewModel<SettingsUiState>(), OneOffEventHandler<SettingsEditGatewayEvent> by OneOffEventHandlerImpl() {

    override fun initialState(): SettingsUiState = SettingsUiState()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            combine(
                getProfileUseCase.gateways,
                getProfileUseCase.security.map {
                    it.isDeveloperModeEnabled
                }
            ) { gateways, isDeveloperModeEnabled ->
                Pair(gateways, isDeveloperModeEnabled)
            }.collect { pair ->
                val current = pair.first.current()
                _state.update { state ->
                    state.copy(
                        currentGateway = current,
                        gatewayList = pair.first.saved.toPersistentList().map {
                            GatewayWrapper(
                                gateway = it,
                                selected = it.url == current.url
                            )
                        }.toPersistentList(),
                        isDeveloperModeEnabled = pair.second
                    )
                }
            }
        }
    }

    fun onNewUrlChanged(newUrl: String) { // check comment in the isValidUrl extension function
        _state.update { state ->
            val sanitizedUrl = newUrl.sanitizeAndValidateGatewayUrl(isDevModeEnabled = state.isDeveloperModeEnabled)
            val urlAlreadyAdded = state.gatewayList.any { it.gateway.url == newUrl || it.gateway.url == sanitizedUrl }
            state.copy(
                newUrlValid = !urlAlreadyAdded && (sanitizedUrl != null && sanitizedUrl.isValidUrl()) && newUrl.isValidUrl(),
                newUrl = newUrl,
                gatewayAddFailure = if (urlAlreadyAdded) GatewayAddFailure.AlreadyExist else null
            )
        }
    }

    fun onDeleteGateway(gateway: GatewayWrapper) {
        viewModelScope.launch {
            if (gateway.selected) {
                val defaultGateway = state.value.gatewayList.first { it.gateway.isDefault }
                switchGateway(defaultGateway.gateway)
            }
            deleteGatewayUseCase(gateway.gateway)
        }
    }

    fun onAddGateway() {
        viewModelScope.launch {
            val newUrl = state.value
                .newUrl
                .sanitizeAndValidateGatewayUrl(isDevModeEnabled = state.value.isDeveloperModeEnabled)
                ?: return@launch

            _state.update { state -> state.copy(addingGateway = true) }

            val newGatewayInfo = networkInfoRepository.getNetworkInfo(newUrl)
            newGatewayInfo.onValue { networkName ->
                addGatewayUseCase(Radix.Gateway(newUrl, Radix.Network.fromName(networkName)))
                _state.update { state ->
                    state.copy(addingGateway = false, newUrl = "", newUrlValid = false)
                }
                sendEvent(SettingsEditGatewayEvent.GatewayAdded)
            }
            newGatewayInfo.onError {
                _state.update { state ->
                    state.copy(
                        gatewayAddFailure = GatewayAddFailure.ErrorWhileAdding,
                        addingGateway = false
                    )
                }
            }
        }
    }

    fun onGatewayClick(gateway: Radix.Gateway) {
        viewModelScope.launch {
            switchGateway(gateway)
        }
    }

    private suspend fun switchGateway(gateway: Radix.Gateway) {
        if (gateway.url == state.value.currentGateway?.url) return

        // this is the case where a url has been added when the developer mode was enabled
        // but at the time the user clicks to switch network the developer mode is disabled
        if (gateway.url.sanitizeAndValidateGatewayUrl(isDevModeEnabled = state.value.isDeveloperModeEnabled) == null) return

        val isGatewayChanged = changeGatewayUseCase(gateway)
        if (isGatewayChanged) {
            _state.update { state -> state.copy(addingGateway = false) }
        } else {
            val urlEncoded = gateway.url.encodeUtf8()
            sendEvent(SettingsEditGatewayEvent.CreateProfileOnNetwork(urlEncoded, gateway.network.name))
        }
    }
}

@VisibleForTesting
internal sealed interface SettingsEditGatewayEvent : OneOffEvent {
    data object GatewayAdded : SettingsEditGatewayEvent
    data class CreateProfileOnNetwork(val newUrl: String, val networkName: String) : SettingsEditGatewayEvent
}

data class SettingsUiState(
    val currentGateway: Radix.Gateway? = null,
    val gatewayList: PersistentList<GatewayWrapper> = persistentListOf(),
    val newUrl: String = "",
    val newUrlValid: Boolean = false,
    val addingGateway: Boolean = false,
    val gatewayAddFailure: GatewayAddFailure? = null,
    val isDeveloperModeEnabled: Boolean = false
) : UiState

enum class GatewayAddFailure {
    AlreadyExist, ErrorWhileAdding
}

data class GatewayWrapper(val gateway: Radix.Gateway, val selected: Boolean)
