package com.babylon.wallet.android.presentation.addfactorsource.name

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceOutput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.SecureStorageAccessErrorKind
import com.radixdlt.sargon.extensions.SharedConstants
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetFactorSourceNameViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val addFactorSourceIOHandler: AddFactorSourceIOHandler,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : StateViewModel<SetFactorSourceNameViewModel.State>(),
    OneOffEventHandler<SetFactorSourceNameViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = SetFactorNameArgs(savedStateHandle)
    private lateinit var addedFactorSourceId: FactorSourceId

    override fun initialState(): State = State(args.factorSourceKind)

    fun onSaveClick() {
        _state.update { state -> state.copy(saveInProgress = true) }

        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                addNewMnemonicFactorSource(
                    factorSourceKind = args.factorSourceKind,
                    mnemonicWithPassphrase = args.mnemonicWithPassphrase,
                    name = state.value.name
                )
            }.onSuccess { factorSourceId ->
                addedFactorSourceId = factorSourceId
                _state.update { state ->
                    state.copy(
                        saveInProgress = false,
                        showSuccess = true
                    )
                }
            }.onFailure { throwable ->
                val uiMessage = when {
                    throwable is CommonException.SecureStorageAccessException &&
                        throwable.errorKind == SecureStorageAccessErrorKind.USER_CANCELLED -> null
                    throwable is CommonException.FileAlreadyExists -> RadixWalletException.AddFactorSource.FactorSourceAlreadyInUse
                    else -> RadixWalletException.AddFactorSource.FactorSourceNotCreated
                }?.let { error -> UiMessage. ErrorMessage(error) }

                _state.update { state ->
                    state.copy(
                        errorMessage = uiMessage,
                        saveInProgress = false
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(name = value)
            }
        }
    }

    fun onDismissMessage() {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(errorMessage = null)
            }
        }
    }

    fun onDismissSuccessMessage() {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(showSuccess = false)
            }

            addFactorSourceIOHandler.setOutput(AddFactorSourceOutput.Id(addedFactorSourceId))
            sendEvent(Event.Saved)
        }
    }

    sealed interface Event : OneOffEvent {

        data object Saved : Event
    }

    data class State(
        val factorSourceKind: FactorSourceKind,
        val saveInProgress: Boolean = false,
        val name: String = "",
        val errorMessage: UiMessage.ErrorMessage? = null,
        val showSuccess: Boolean = false
    ) : UiState {

        val isNameTooLong = name.trim().length > SharedConstants.displayNameMaxLength
        val isButtonEnabled = name.isNotBlank() && !isNameTooLong
    }
}
