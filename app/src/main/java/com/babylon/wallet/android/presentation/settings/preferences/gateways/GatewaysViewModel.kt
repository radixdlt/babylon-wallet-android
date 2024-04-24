package com.babylon.wallet.android.presentation.settings.preferences.gateways

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.isValidUrl
import com.babylon.wallet.android.utils.sanitizeAndValidateGatewayUrl
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Url
import com.radixdlt.sargon.extensions.all
import com.radixdlt.sargon.extensions.init
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.default
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.AddGatewayUseCase
import rdx.works.profile.domain.gateway.ChangeGatewayIfNetworkExistUseCase
import rdx.works.profile.domain.gateway.DeleteGatewayUseCase
import javax.inject.Inject

@HiltViewModel
class GatewaysViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val changeGatewayIfNetworkExistUseCase: ChangeGatewayIfNetworkExistUseCase,
    private val addGatewayUseCase: AddGatewayUseCase,
    private val deleteGatewayUseCase: DeleteGatewayUseCase,
    private val getNetworkInfoUseCase: GetNetworkInfoUseCase,
) : StateViewModel<SettingsUiState>(), OneOffEventHandler<SettingsEditGatewayEvent> by OneOffEventHandlerImpl() {

    override fun initialState(): SettingsUiState = SettingsUiState()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            combine(
                getProfileUseCase.flow.map { it.appPreferences.gateways },
                getProfileUseCase.flow.map { it.appPreferences.security.isDeveloperModeEnabled }
            ) { gateways, isDeveloperModeEnabled ->
                Pair(gateways, isDeveloperModeEnabled)
            }.collect { pair ->
                val current = pair.first.current
                _state.update { state ->
                    state.copy(
                        currentGateway = current,
                        gatewayList = pair.first.all.toPersistentList().map {
                            GatewayWrapper(
                                gateway = it,
                                selected = it == current // TODO Integration
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
            // if sanitizedUrl = null it means it didn't pass the validation
            val sanitizedUrl = newUrl.sanitizeAndValidateGatewayUrl(isDevModeEnabled = state.isDeveloperModeEnabled)
            val urlAlreadyAdded = state.gatewayList.map { it.gateway.url.toString() }.any { it == newUrl || it == sanitizedUrl }
            state.copy(
                newUrlValid = !urlAlreadyAdded && sanitizedUrl?.isValidUrl() == true && newUrl.isValidUrl(),
                newUrl = newUrl,
                gatewayAddFailure = if (urlAlreadyAdded) GatewayAddFailure.AlreadyExist else null
            )
        }
    }

    fun onDeleteGateway(gateway: GatewayWrapper) {
        viewModelScope.launch {
            if (gateway.selected) {
                val defaultGateway = state.value.gatewayList.first { it.gateway.network.id == Gateway.default.network.id }
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

            getNetworkInfoUseCase(newUrl)
                .onSuccess { info ->
                    addGatewayUseCase(Gateway.init(newUrl, info.id))
                    _state.update { state ->
                        state.copy(addingGateway = false, newUrl = "", newUrlValid = false)
                    }
                    sendEvent(SettingsEditGatewayEvent.GatewayAdded)
                }.onFailure {
                    _state.update { state ->
                        state.copy(
                            gatewayAddFailure = GatewayAddFailure.ErrorWhileAdding,
                            addingGateway = false
                        )
                    }
                }
        }
    }

    fun onGatewayClick(gateway: Gateway) {
        viewModelScope.launch {
            switchGateway(gateway)
        }
    }

    private suspend fun switchGateway(gateway: Gateway) {
        if (gateway == state.value.currentGateway) return // TODO integration

        // this is the case where a url has been added when the developer mode was enabled
        // but at the time the user clicks to switch network the developer mode is disabled
        if (gateway.url.toString().sanitizeAndValidateGatewayUrl(isDevModeEnabled = state.value.isDeveloperModeEnabled) == null) return

        val isGatewayChanged = changeGatewayIfNetworkExistUseCase(gateway)
        if (isGatewayChanged) {
            _state.update { state -> state.copy(addingGateway = false) }
        } else {
            sendEvent(SettingsEditGatewayEvent.CreateProfileOnNetwork(gateway.url, gateway.network.id))
        }
    }

    fun setAddGatewaySheetVisible(isVisible: Boolean) {
        _state.update { it.copy(isAddGatewaySheetVisible = isVisible) }
    }
}

internal sealed interface SettingsEditGatewayEvent : OneOffEvent {
    data object GatewayAdded : SettingsEditGatewayEvent
    data class CreateProfileOnNetwork(val newUrl: Url, val networkId: NetworkId) : SettingsEditGatewayEvent
}

data class SettingsUiState(
    val currentGateway: Gateway? = null,
    val gatewayList: PersistentList<GatewayWrapper> = persistentListOf(),
    val newUrl: String = "",
    val newUrlValid: Boolean = false,
    val addingGateway: Boolean = false,
    val gatewayAddFailure: GatewayAddFailure? = null,
    val isDeveloperModeEnabled: Boolean = false,
    val isAddGatewaySheetVisible: Boolean = false
) : UiState

enum class GatewayAddFailure {
    AlreadyExist, ErrorWhileAdding
}

data class GatewayWrapper(val gateway: Gateway, val selected: Boolean)
