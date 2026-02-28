package com.babylon.wallet.android.presentation.settings.preferences.ss.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.P2pStunServer
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.P2pTurnServer
import com.radixdlt.sargon.extensions.all
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignalingServerDetailsViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : StateViewModel<SignalingServerDetailsViewModel.State>(),
    OneOffEventHandler<SignalingServerDetailsViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = SignalingServerDetailsNavArgs(savedStateHandle)
    private lateinit var p2pTransportProfile: P2pTransportProfile

    init {
        args.id?.let { loadDetails(it) }
    }

    override fun initialState(): State = State()

    fun onNameChanged(value: String) {
        _state.update { state ->
            state.copy(
                name = value
            )
        }

        invalidateSaveButton()
    }

    fun onSignalingServerUrlChanged(value: String) {
        _state.update { state ->
            state.copy(
                url = value
            )
        }

        invalidateSaveButton()
    }

    fun onStunUrlChanged(index: Int, value: String) {
        _state.update { state ->
            state.copy(
                stunUrls = updateUrls(state.stunUrls, index, value).toPersistentList()
            )
        }

        invalidateSaveButton()
    }

    fun onTurnUrlChanged(index: Int, value: String) {
        _state.update { state ->
            state.copy(
                turnUrls = updateUrls(state.turnUrls, index, value).toPersistentList()
            )
        }

        invalidateSaveButton()
    }

    fun onDeleteStunUrlClick(index: Int) {
        _state.update { state ->
            state.copy(
                stunUrls = deleteUrl(state.stunUrls, index).toPersistentList()
            )
        }

        invalidateSaveButton()
    }

    fun onDeleteTurnUrlClick(index: Int) {
        _state.update { state ->
            state.copy(
                turnUrls = deleteUrl(state.turnUrls, index).toPersistentList()
            )
        }

        invalidateSaveButton()
    }

    fun onAddStunUrlClick() {
        _state.update { state ->
            state.copy(
                stunUrls = addUrl(state.stunUrls).toPersistentList()
            )
        }

        invalidateSaveButton()
    }

    fun onAddTurnUrlClick() {
        _state.update { state ->
            state.copy(
                turnUrls = addUrl(state.turnUrls).toPersistentList()
            )
        }

        invalidateSaveButton()
    }

    fun onTurnUsernameChanged(value: String) {
        _state.update { state ->
            state.copy(
                turnUsername = value
            )
        }

        invalidateSaveButton()
    }

    fun onTurnPasswordChanged(value: String) {
        _state.update { state ->
            state.copy(
                turnPassword = value
            )
        }

        invalidateSaveButton()
    }

    fun onChangeAsCurrent() {
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                changeCurrentP2pTransportProfile(p2pTransportProfile)
            }.onFailure {
                onError(it)
            }.onSuccess { changed ->
                if (changed) {
                    loadDetails(p2pTransportProfile.id)
                } else {
                    onError("Failed to change server")
                }
            }
        }
    }

    fun onDeleteClick() {
        _state.update { state ->
            state.copy(
                showDeleteConfirmation = true
            )
        }
    }

    fun onDeleteConfirmationDismissed(confirmed: Boolean) {
        _state.update { state ->
            state.copy(
                showDeleteConfirmation = false
            )
        }

        if (confirmed) {
            viewModelScope.launch {
                sargonOsManager.callSafely(defaultDispatcher) {
                    deleteP2pTransportProfile(p2pTransportProfile)
                }.onFailure {
                    onError(it)
                }.onSuccess { deleted ->
                    if (deleted) {
                        sendEvent(Event.Dismiss)
                    } else {
                        onError("Failed to delete server")
                    }
                }
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

    fun onSaveClick() {
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                val p2pTransportProfile = state.value.toP2pTransportProfile()

                val saved = if (state.value.isInEditMode) {
                    updateP2pTransportProfile(p2pTransportProfile)
                } else {
                    addP2pTransportProfile(p2pTransportProfile)
                }

                if (saved) {
                    loadDetails(p2pTransportProfile.id)
                } else {
                    onError("Failed to save changes")
                }
            }.onFailure {
                onError(it)
            }
        }
    }

    private fun loadDetails(id: String) {
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                val profiles = p2pTransportProfiles()
                p2pTransportProfile = profiles.all.firstOrNull { it.id == id }
                    ?: error("Server with ${args.id} id not found")
                val isCurrent = p2pTransportProfile == profiles.current

                State(
                    isInEditMode = true,
                    isSaveEnabled = false,
                    name = p2pTransportProfile.name,
                    url = p2pTransportProfile.signalingServer,
                    stunUrls = p2pTransportProfile.stun.urls.toPersistentList(),
                    turnUrls = p2pTransportProfile.turn.urls.toPersistentList(),
                    turnUsername = p2pTransportProfile.turn.username.orEmpty(),
                    turnPassword = p2pTransportProfile.turn.credential.orEmpty(),
                    isCurrent = isCurrent
                )
            }.onFailure {
                onError(it)
            }.onSuccess {
                _state.emit(it)
            }
        }
    }

    private fun State.toP2pTransportProfile(): P2pTransportProfile {
        return P2pTransportProfile(
            name = name,
            signalingServer = url,
            stun = P2pStunServer(
                urls = stunUrls
            ),
            turn = P2pTurnServer(
                username = turnUsername.takeIf { it.isNotBlank() && turnUrls.isNotEmpty() },
                credential = turnPassword.takeIf { it.isNotBlank() && turnUrls.isNotEmpty() },
                urls = turnUrls
            )
        )
    }

    private fun updateUrls(urls: List<String>, index: Int, value: String): List<String> {
        return urls.mapIndexed { i, url ->
            if (i == index) {
                value
            } else {
                url
            }
        }
    }

    private fun deleteUrl(urls: List<String>, index: Int): List<String> {
        return urls.filterIndexed { i, _ -> i != index }
    }

    private fun addUrl(urls: List<String>): List<String> {
        return urls + ""
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

    private fun invalidateSaveButton() {
        _state.update { state ->
            state.copy(
                isSaveEnabled = canSave()
            )
        }
    }

    private fun canSave(): Boolean {
        return if (state.value.isInEditMode) {
            state.value.name != p2pTransportProfile.name ||
                state.value.url != p2pTransportProfile.signalingServer ||
                state.value.stunUrls != p2pTransportProfile.stun.urls ||
                state.value.turnUrls != p2pTransportProfile.turn.urls ||
                state.value.turnUsername != p2pTransportProfile.turn.username ||
                state.value.turnPassword != p2pTransportProfile.turn.credential
        } else {
            state.value.name.isNotBlank() &&
                state.value.url.isNotBlank() &&
                state.value.stunUrls.all { it.isNotBlank() } &&
                state.value.turnUrls.all { it.isNotBlank() }
        }
    }

    data class State(
        val isInEditMode: Boolean = false,
        val isSaveEnabled: Boolean = false,
        val name: String = "",
        val url: String = "",
        val stunUrls: ImmutableList<String> = persistentListOf(),
        val turnUsername: String = "",
        val turnPassword: String = "",
        val turnUrls: ImmutableList<String> = persistentListOf(),
        val isCurrent: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null,
        val showDeleteConfirmation: Boolean = false
    ) : UiState {

        val canAddMoreStunUrls: Boolean = stunUrls.all { it.isNotBlank() }
        val canAddMoreTurnUrls: Boolean = turnUrls.all { it.isNotBlank() }
    }

    sealed interface Event : OneOffEvent {

        data object Dismiss : Event
    }
}
