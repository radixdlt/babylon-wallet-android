package com.babylon.wallet.android.presentation.settings.preferences.rs

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.preferences.rs.RelayServicesViewModel.State.AddInput
import com.babylon.wallet.android.utils.callSafely
import com.babylon.wallet.android.utils.isValidUrl
import com.babylon.wallet.android.utils.sanitizeAndValidateGatewayUrl
import com.radixdlt.sargon.RelayService
import com.radixdlt.sargon.extensions.toUrl
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class RelayServicesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<RelayServicesViewModel.State>() {

    init {
        observeServices()
    }

    override fun initialState(): State = State()

    fun onItemClick(item: State.UiItem) {
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                changeCurrentRelayService(item.relayService)
            }.onFailure {
                onError(it)
            }
        }
    }

    fun onAddConfirmed() {
        viewModelScope.launch {
            val addInput = state.value.addInput ?: return@launch
            val newUrl = addInput.url.sanitizeAndValidateGatewayUrl(
                isDevModeEnabled = state.value.isDeveloperModeEnabled
            ) ?: return@launch

            _state.update { state ->
                state.copy(
                    addInput = state.addInput?.copy(
                        isLoading = true
                    )
                )
            }

            sargonOsManager.callSafely(defaultDispatcher) {
                addRelayService(
                    RelayService(
                        name = addInput.name,
                        url = newUrl.toUrl()
                    )
                )
                setAddSheetVisible(false)
            }.onFailure {
                onError(it)
            }
        }
    }

    fun onDismissErrorMessage() {
        _state.update { state ->
            state.copy(
                errorMessage = null
            )
        }
    }

    fun onDeleteItemClick(item: State.UiItem) {
        _state.update { state ->
            state.copy(
                itemToDelete = item
            )
        }
    }

    fun onNewNameChanged(newName: String) {
        _state.update { state ->
            state.copy(
                addInput = state.addInput?.copy(
                    name = newName
                )
            )
        }
    }

    fun onNewUrlChanged(newUrl: String) {
        _state.update { state ->
            val sanitizedUrl = newUrl.sanitizeAndValidateGatewayUrl(
                isDevModeEnabled = state.isDeveloperModeEnabled
            )
            val urlAlreadyAdded = state.items.map { it.relayService.url.toString() }
                .any { it == newUrl || it == sanitizedUrl }

            state.copy(
                addInput = state.addInput?.copy(
                    isUrlValid = !urlAlreadyAdded && sanitizedUrl?.isValidUrl() == true && newUrl.isValidUrl(),
                    url = newUrl,
                    failure = AddInput.Failure.AlreadyExist.takeIf { urlAlreadyAdded }
                )
            )
        }
    }

    fun setAddSheetVisible(isVisible: Boolean) {
        _state.update { state ->
            state.copy(
                addInput = AddInput().takeIf { isVisible }
            )
        }
    }

    fun onDeleteConfirmationDismissed(confirmed: Boolean) {
        val itemToDelete = state.value.itemToDelete ?: return

        _state.update { state ->
            state.copy(
                itemToDelete = null
            )
        }

        if (confirmed) {
            viewModelScope.launch {
                sargonOsManager.callSafely(defaultDispatcher) {
                    deleteRelayService(itemToDelete.relayService)
                }.onFailure {
                    onError(it)
                }.onSuccess { deleted ->
                    if (!deleted) {
                        onError("Failed to delete relay service")
                    }
                }
            }
        }
    }

    private fun observeServices() {
        getProfileUseCase.flow
            .onEach { profile ->
                val relayServices = profile.appPreferences.relayServices
                _state.update { state ->
                    state.copy(
                        items = (relayServices.other + relayServices.current)
                            .sortedBy { it.name }
                            .map {
                                State.UiItem(
                                    relayService = it,
                                    isCurrent = it == relayServices.current
                                )
                            },
                        isDeveloperModeEnabled = profile.appPreferences.security.isDeveloperModeEnabled
                    )
                }
            }.launchIn(viewModelScope)
    }

    private fun onError(message: String) {
        onError(Throwable(message))
    }

    private fun onError(throwable: Throwable) {
        _state.update { state ->
            state.copy(
                errorMessage = UiMessage.ErrorMessage(throwable)
            )
        }
    }

    data class State(
        val items: List<UiItem> = emptyList(),
        val isDeveloperModeEnabled: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null,
        val addInput: AddInput? = null,
        val itemToDelete: UiItem? = null
    ) : UiState {

        data class UiItem(
            val relayService: RelayService,
            val isCurrent: Boolean
        )

        data class AddInput(
            val name: String = "",
            val url: String = "",
            val isUrlValid: Boolean = false,
            val isLoading: Boolean = false,
            val failure: Failure? = null
        ) {

            enum class Failure {
                AlreadyExist
            }
        }
    }
}
