package com.babylon.wallet.android.presentation.settings.preferences.gateways

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
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
import com.radixdlt.sargon.extensions.all
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.isWellKnown
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.comparator
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
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<GatewaysViewModel.State>(), OneOffEventHandler<GatewaysViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            combine(
                getProfileUseCase.flow.map { it.appPreferences.gateways },
                getProfileUseCase.flow.map { it.appPreferences.security.isDeveloperModeEnabled }
            ) { gateways, isDeveloperModeEnabled ->
                state.value.copy(
                    currentGateway = gateways.current,
                    gatewayList = gateways.all
                        .sortedWith(Gateway.comparator)
                        .toPersistentList().map {
                            State.GatewayUiItem(
                                gateway = it,
                                selected = it == gateways.current
                            )
                        }.toPersistentList(),
                    isDeveloperModeEnabled = isDeveloperModeEnabled,
                    addGatewayInput = null
                )
            }
                .flowOn(defaultDispatcher)
                .collect { state ->
                    println("Profile emitted")
                    _state.emit(state)
                }
        }
    }

    fun onNewUrlChanged(newUrl: String) { // check comment in the isValidUrl extension function
        _state.update { state ->
            // if sanitizedUrl = null it means it didn't pass the validation
            val sanitizedUrl = newUrl.sanitizeAndValidateGatewayUrl(isDevModeEnabled = state.isDeveloperModeEnabled)
            val urlAlreadyAdded = state.gatewayList.map { it.gateway.string }.any { it == newUrl || it == sanitizedUrl }
            state.copy(
                addGatewayInput = state.addGatewayInput?.copy(
                    isUrlValid = !urlAlreadyAdded && sanitizedUrl?.isValidUrl() == true && newUrl.isValidUrl(),
                    url = newUrl,
                    failure = State.AddGatewayInput.Failure.AlreadyExist.takeIf { urlAlreadyAdded }
                )
            )
        }
    }

    fun onDeleteGateway(gateway: State.GatewayUiItem) {
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
            val newUrl = state.value.addGatewayInput?.url
                ?.sanitizeAndValidateGatewayUrl(isDevModeEnabled = state.value.isDeveloperModeEnabled)
                ?: return@launch

            _state.update { state ->
                state.copy(
                    addGatewayInput = state.addGatewayInput?.copy(
                        isLoading = true
                    )
                )
            }

            getNetworkInfoUseCase(newUrl)
                .onSuccess { info ->
                    addGatewayUseCase(Gateway.init(newUrl, info.id))
                    setAddGatewaySheetVisible(false)
                }.onFailure {
                    _state.update { state ->
                        state.copy(
                            addGatewayInput = state.addGatewayInput?.copy(
                                failure = State.AddGatewayInput.Failure.ErrorWhileAdding,
                                isLoading = false
                            )
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
        if (gateway == state.value.currentGateway) return

        // this is the case where a url has been added when the developer mode was enabled
        // but at the time the user clicks to switch network the developer mode is disabled
        if (gateway.string.sanitizeAndValidateGatewayUrl(isDevModeEnabled = state.value.isDeveloperModeEnabled) == null) return

        val isGatewayChanged = changeGatewayIfNetworkExistUseCase(gateway)
        if (isGatewayChanged) {
            setAddGatewaySheetVisible(false)
        } else {
            sendEvent(Event.CreateAccountOnNetwork(gateway.network.id))
        }
    }

    fun setAddGatewaySheetVisible(isVisible: Boolean) {
        _state.update { it.setAddGatewaySheetVisible(isVisible) }
    }

    internal sealed interface Event : OneOffEvent {
        data class CreateAccountOnNetwork(val networkId: NetworkId) : Event
    }

    data class State(
        val currentGateway: Gateway? = null,
        val gatewayList: PersistentList<GatewayUiItem> = persistentListOf(),
        val isDeveloperModeEnabled: Boolean = false,
        val addGatewayInput: AddGatewayInput? = null
    ) : UiState {

        fun setAddGatewaySheetVisible(isVisible: Boolean) = copy(
            addGatewayInput = AddGatewayInput().takeIf { isVisible }
        )

        data class GatewayUiItem(
            val gateway: Gateway,
            val selected: Boolean
        ) {

            val isWellKnown: Boolean = gateway.isWellKnown
            val url = gateway.string
        }

        data class AddGatewayInput(
            val url: String = "",
            val isUrlValid: Boolean = false,
            val isLoading: Boolean = false,
            val failure: Failure? = null
        ) {

            enum class Failure {
                AlreadyExist, ErrorWhileAdding
            }
        }
    }
}
