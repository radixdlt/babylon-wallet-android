package com.babylon.wallet.android.presentation.settings.preferences.tokenpriceservices

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.babylon.wallet.android.utils.sanitizeAndValidateGatewayUrl
import com.radixdlt.sargon.TokenPriceService
import com.radixdlt.sargon.extensions.toUrl
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class TokenPriceServicesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<TokenPriceServicesViewModel.State>() {

    init {
        observeServices()
    }

    override fun initialState(): State = State()

    fun onAddConfirmed() {
        viewModelScope.launch {
            val addInput = state.value.addInput ?: return@launch
            val newUrl = addInput.url.sanitizeAndValidateTokenPriceServiceUrl(
                isDevModeEnabled = state.value.isDeveloperModeEnabled
            ) ?: return@launch

            _state.update { currentState ->
                currentState.copy(
                    addInput = currentState.addInput?.copy(isLoading = true)
                )
            }

            sargonOsManager.callSafely(defaultDispatcher) {
                addTokenPriceServiceOnCurrentNetwork(newUrl.toUrl())
            }.onFailure {
                onError(it)
                _state.update { currentState ->
                    currentState.copy(
                        addInput = currentState.addInput?.copy(isLoading = false)
                    )
                }
            }.onSuccess { added ->
                if (added) {
                    setAddSheetVisible(false)
                } else {
                    _state.update { currentState ->
                        currentState.copy(
                            addInput = currentState.addInput?.copy(
                                failure = State.AddInput.Failure.ErrorWhileAdding,
                                isLoading = false
                            )
                        )
                    }
                }
            }
        }
    }

    fun onDeleteItemClick(item: State.UiItem) {
        if (state.value.items.size <= 1) return

        _state.update { currentState ->
            currentState.copy(itemToDelete = item)
        }
    }

    fun onDeleteConfirmationDismissed(confirmed: Boolean) {
        val itemToDelete = state.value.itemToDelete ?: return

        _state.update { currentState ->
            currentState.copy(itemToDelete = null)
        }

        if (confirmed) {
            viewModelScope.launch {
                sargonOsManager.callSafely(defaultDispatcher) {
                    deleteTokenPriceServiceOnCurrentNetwork(itemToDelete.tokenPriceService.baseUrl)
                }.onFailure {
                    onError(it)
                }.onSuccess { deleted ->
                    if (!deleted) {
                        onError("Failed to delete token price service")
                    }
                }
            }
        }
    }

    fun onDismissErrorMessage() {
        _state.update { currentState ->
            currentState.copy(errorMessage = null)
        }
    }

    fun onNewUrlChanged(newUrl: String) {
        _state.update { currentState ->
            val sanitizedUrl = newUrl.sanitizeAndValidateTokenPriceServiceUrl(
                isDevModeEnabled = currentState.isDeveloperModeEnabled
            )
            val urlAlreadyAdded = currentState.items.map { it.tokenPriceService.baseUrl.toString() }
                .any { it == newUrl || it == sanitizedUrl }

            currentState.copy(
                addInput = currentState.addInput?.copy(
                    isUrlValid = !urlAlreadyAdded && sanitizedUrl != null,
                    url = newUrl,
                    failure = State.AddInput.Failure.AlreadyExist.takeIf { urlAlreadyAdded }
                )
            )
        }
    }

    fun setAddSheetVisible(isVisible: Boolean) {
        _state.update { currentState ->
            currentState.copy(addInput = State.AddInput().takeIf { isVisible })
        }
    }

    private fun observeServices() {
        getProfileUseCase.flow
            .onEach { profile ->
                _state.update { currentState ->
                    currentState.copy(
                        items = profile.currentNetwork
                            ?.tokenPriceServices
                            .orEmpty()
                            .sortedBy { it.baseUrl.toString() }
                            .map { State.UiItem(tokenPriceService = it) },
                        isDeveloperModeEnabled = profile.appPreferences.security.isDeveloperModeEnabled
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onError(message: String) {
        onError(Throwable(message))
    }

    private fun onError(throwable: Throwable) {
        _state.update { currentState ->
            currentState.copy(errorMessage = UiMessage.ErrorMessage(throwable))
        }
    }

    private fun String.sanitizeAndValidateTokenPriceServiceUrl(isDevModeEnabled: Boolean): String? {
        val trimmed = trim()
        if (trimmed.isEmpty()) return null

        val urlWithScheme = when {
            trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
            isDevModeEnabled -> "http://$trimmed"
            else -> "https://$trimmed"
        }

        val parsed = runCatching { urlWithScheme.toUrl() }.getOrNull() ?: return null
        val formattedHost = if (parsed.host.contains(':')) {
            "[${parsed.host}]"
        } else {
            parsed.host
        }
        val hasCustomPort = parsed.port != when (parsed.scheme) {
            "http" -> 80
            "https" -> 443
            else -> parsed.port
        }
        val origin = buildString {
            append(parsed.scheme)
            append("://")
            append(formattedHost)
            if (hasCustomPort) {
                append(":")
                append(parsed.port)
            }
            append("/")
        }

        if (origin.sanitizeAndValidateGatewayUrl(isDevModeEnabled) == null) return null

        return parsed.toString()
    }

    data class State(
        val items: List<UiItem> = emptyList(),
        val isDeveloperModeEnabled: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null,
        val addInput: AddInput? = null,
        val itemToDelete: UiItem? = null
    ) : UiState {

        data class UiItem(
            val tokenPriceService: TokenPriceService
        )

        data class AddInput(
            val url: String = "",
            val isUrlValid: Boolean = false,
            val isLoading: Boolean = false,
            val failure: Failure? = null
        ) {
            enum class Failure {
                AlreadyExist,
                ErrorWhileAdding
            }
        }
    }
}
