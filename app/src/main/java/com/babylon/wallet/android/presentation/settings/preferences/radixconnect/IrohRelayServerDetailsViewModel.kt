package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServer
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServerException
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelaysRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.peerdroid.data.iroh.IrohRelayConfig
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class IrohRelayServerDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val irohRelaysRepository: IrohRelaysRepository,
    private val irohRelayConfig: IrohRelayConfig,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<IrohRelayServerDetailsViewModel.State>(),
    OneOffEventHandler<IrohRelayServerDetailsViewModel.Event> by OneOffEventHandlerImpl() {

    private val relayServer = IrohRelayServerDetailsArgs(savedStateHandle).relayServer

    private var validationJob: Job? = null

    // The custom relay servers listed, so that one already there is pointed out while it is typed.
    private var listedUrls: Set<String> = emptySet()

    override fun initialState(): State = State(
        relayServer = relayServer,
        relayUrl = (relayServer as? IrohRelayServer.Custom)?.url.orEmpty()
    )

    init {
        irohRelaysRepository.relayServers.onEach { relayServers ->
            listedUrls = (relayServers.others + relayServers.current)
                .mapNotNull { (it as? IrohRelayServer.Custom)?.url }
                .toSet()
            _state.update { state -> state.copy(isCurrent = relayServer != null && relayServers.current == relayServer) }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            // The first call loads the native library: keep it off the main thread.
            val publicRelayUrls = withContext(defaultDispatcher) {
                runCatching { irohRelayConfig.publicRelayUrls() }
                    .onFailure { error -> Timber.w("iroh library unavailable: ${error.javaClass.simpleName}") }
                    .getOrNull()
            }
            _state.update { state ->
                state.copy(
                    isLoaded = true,
                    isLibraryAvailable = publicRelayUrls != null,
                    publicRelayUrls = publicRelayUrls?.toPersistentList()
                )
            }
        }
    }

    fun onRelayUrlChanged(value: String) {
        // The error and Save stay as they were until this URL is validated: cleared on every key and set again a
        // moment later, they flickered while typing.
        _state.update { state -> state.copy(relayUrl = value) }
        validate(value)
    }

    fun onSaveClick() {
        // Enabled only once exactly this URL was validated.
        val state = _state.value
        if (!state.isSaveEnabled || state.validatedUrl != state.relayUrl) return

        viewModelScope.launch {
            val result = when (relayServer) {
                null -> irohRelaysRepository.add(state.relayUrl)
                is IrohRelayServer.Custom -> irohRelaysRepository.update(relayServer, state.relayUrl)
                IrohRelayServer.Public -> return@launch
            }
            result.onSuccess { sendEvent(Event.Dismiss) }.onFailure(::onFailure)
        }
    }

    fun onMakeCurrentClick() {
        val relayServer = relayServer ?: return
        viewModelScope.launch {
            irohRelaysRepository.makeCurrent(relayServer)
                .onSuccess { sendEvent(Event.Dismiss) }
                .onFailure(::onFailure)
        }
    }

    fun onDeleteClick() {
        _state.update { state -> state.copy(showDeleteConfirmation = true) }
    }

    fun onDeleteConfirmationDismissed(confirmed: Boolean) {
        _state.update { state -> state.copy(showDeleteConfirmation = false) }
        val relayServer = relayServer
        if (!confirmed || relayServer == null) return

        viewModelScope.launch {
            irohRelaysRepository.remove(relayServer)
                .onSuccess { sendEvent(Event.Dismiss) }
                .onFailure(::onFailure)
        }
    }

    fun onDismissErrorMessage() {
        _state.update { state -> state.copy(errorMessage = null) }
    }

    // Validated off the main thread by the iroh library, once it is known to be available.
    private fun validate(relayUrl: String) {
        validationJob?.cancel()
        if (!_state.value.isLibraryAvailable) return

        validationJob = viewModelScope.launch {
            // Once typing pauses: the URLs on the way to the one meant are not worth an error.
            delay(VALIDATION_DELAY_MS)
            val result = irohRelaysRepository.validate(relayUrl)
            _state.update { current ->
                if (current.relayUrl != relayUrl) {
                    current
                } else {
                    val ownUrl = (relayServer as? IrohRelayServer.Custom)?.url
                    val validUrl = result.getOrNull()
                    val error = result.exceptionOrNull()
                    val alreadyAdded = validUrl != null && validUrl != ownUrl && validUrl in listedUrls
                    current.copy(
                        validatedUrl = relayUrl,
                        fieldError = when {
                            error != null -> error.toFieldError() ?: FieldError.Invalid(error.describe())
                            alreadyAdded -> FieldError.AlreadyAdded
                            else -> null
                        },
                        isSaveEnabled = validUrl != null && validUrl != ownUrl && !alreadyAdded
                    )
                }
            }
        }
    }

    private fun onFailure(error: Throwable) {
        val fieldError = error.toFieldError()
        _state.update { state ->
            if (fieldError != null) {
                state.copy(fieldError = fieldError, isSaveEnabled = false)
            } else {
                state.copy(errorMessage = UiMessage.ErrorMessage(error))
            }
        }
    }

    private fun Throwable.toFieldError(): FieldError? = when (this) {
        is IrohRelayServerException.InvalidUrl -> FieldError.Invalid(reason)
        is IrohRelayServerException.AlreadyAdded -> FieldError.AlreadyAdded
        else -> null
    }

    private fun Throwable.describe(): String = message ?: javaClass.simpleName

    sealed interface FieldError {
        data class Invalid(val reason: String) : FieldError
        data object AlreadyAdded : FieldError
    }

    data class State(
        // Null for a new relay server.
        val relayServer: IrohRelayServer?,
        // What the user types.
        val relayUrl: String = "",
        val isLoaded: Boolean = false,
        val isLibraryAvailable: Boolean = false,
        // n0's public relays, for display.
        val publicRelayUrls: ImmutableList<String>? = null,
        val isCurrent: Boolean = false,
        // The URL the error and Save are about: Save saves nothing else.
        val validatedUrl: String? = null,
        val fieldError: FieldError? = null,
        val isSaveEnabled: Boolean = false,
        val showDeleteConfirmation: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val isPublic: Boolean
            get() = relayServer == IrohRelayServer.Public

        val isEditable: Boolean
            get() = !isPublic && isLoaded && isLibraryAvailable

        val canBeRemoved: Boolean
            get() = relayServer is IrohRelayServer.Custom

        val canBeMadeCurrent: Boolean
            get() = relayServer != null && !isCurrent
    }

    sealed interface Event : OneOffEvent {

        data object Dismiss : Event
    }
}

private const val VALIDATION_DELAY_MS = 250L
