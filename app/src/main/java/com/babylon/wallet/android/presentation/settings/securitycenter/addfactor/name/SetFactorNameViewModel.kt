package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.name

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.SecureStorageAccessErrorKind
import com.radixdlt.sargon.extensions.SharedConstants
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SetFactorNameViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : StateViewModel<SetFactorNameViewModel.State>(),
    OneOffEventHandler<SetFactorNameViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = SetFactorNameArgs(savedStateHandle)

    override fun initialState(): State = State(args.factorSourceKind)

    fun onSaveClick() {
        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                addNewMnemonicFactorSource(
                    factorSourceKind = args.factorSourceKind,
                    mnemonicWithPassphrase = args.mnemonicWithPassphrase,
                    name = state.value.name
                )
            }.onSuccess { factorSourceId ->
                Timber.d("FactorSourceID: $factorSourceId")
                sendEvent(Event.Saved)
            }.onFailure { throwable ->
                when {
                    throwable is CommonException.SecureStorageAccessException &&
                        throwable.errorKind == SecureStorageAccessErrorKind.USER_CANCELLED -> null
                    throwable is CommonException.FileAlreadyExists -> RadixWalletException.AddFactorSource.FactorSourceAlreadyInUse
                    else -> RadixWalletException.AddFactorSource.FactorSourceNotCreated
                }?.let { error ->
                    _state.update { state ->
                        state.copy(
                            errorMessage = UiMessage.ErrorMessage(error)
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(name = value.trim())
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

    sealed interface Event : OneOffEvent {

        data object Saved : Event
    }

    data class State(
        val factorSourceKind: FactorSourceKind,
        val name: String = "",
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val isNameTooLong = name.trim().length > SharedConstants.displayNameMaxLength
        val isButtonEnabled = name.isNotBlank() && !isNameTooLong
    }
}
