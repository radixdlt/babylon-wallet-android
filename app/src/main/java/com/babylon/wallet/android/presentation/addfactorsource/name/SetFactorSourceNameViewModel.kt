package com.babylon.wallet.android.presentation.addfactorsource.name

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceOutput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.DeviceFactorSourceType
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.SecureStorageAccessErrorKind
import com.radixdlt.sargon.extensions.SharedConstants
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.name
import com.radixdlt.sargon.extensions.then
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.init
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.ProfileException
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SetFactorSourceNameViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val addFactorSourceIOHandler: AddFactorSourceIOHandler,
    private val preferencesManager: PreferencesManager,
    private val mnemonicRepository: MnemonicRepository,
    private val biometricsAuthenticateUseCase: BiometricsAuthenticateUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : StateViewModel<SetFactorSourceNameViewModel.State>(),
    OneOffEventHandler<SetFactorSourceNameViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = SetFactorNameArgs.from(savedStateHandle)
    private val input = addFactorSourceIOHandler.getInput() as AddFactorSourceInput.WithKindPreselected

    private lateinit var addedFactorSourceId: FactorSourceId

    override fun initialState(): State = State(args.factorSourceKind)

    fun onSaveClick() {
        _state.update { state -> state.copy(saveInProgress = true) }

        viewModelScope.launch {
            saveFactorSource().onSuccess { factorSourceId ->
                addedFactorSourceId = factorSourceId

                _state.update { state ->
                    state.copy(
                        saveInProgress = false,
                        showSuccess = true
                    )
                }
            }.onFailure { throwable ->
                val uiMessage = if (throwable is CommonException) {
                    when {
                        throwable is CommonException.SecureStorageAccessException &&
                            throwable.errorKind == SecureStorageAccessErrorKind.USER_CANCELLED -> null

                        else -> RadixWalletException.AddFactorSource.FactorSourceNotCreated
                    }
                } else {
                    throwable
                }?.let { error ->
                    UiMessage.ErrorMessage(error)
                }

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

    private suspend fun saveFactorSource(): Result<FactorSourceId> = when (args) {
        is SetFactorNameArgs.ForLedger -> addFactorSource(
            FactorSource.Ledger.init(
                id = args.factorSourceId,
                model = args.ledgerModel,
                name = state.value.name
            )
        )

        is SetFactorNameArgs.WithMnemonic -> when (args.factorSourceKind) {
            FactorSourceKind.DEVICE -> saveDeviceFactorSource(args)
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> error("Shouldn't be here")
            FactorSourceKind.OFF_DEVICE_MNEMONIC,
            FactorSourceKind.ARCULUS_CARD,
            FactorSourceKind.PASSWORD -> error("Not yet supported")
        }
    }

    private suspend fun saveDeviceFactorSource(
        args: SetFactorNameArgs.WithMnemonic
    ): Result<FactorSourceId> {
        val factorSource = sargonOsManager.sargonOs.createDeviceFactorSource(
            mnemonicWithPassphrase = args.mnemonicWithPassphrase,
            factorType = when (input.context) {
                AddFactorSourceInput.Context.New -> DeviceFactorSourceType.BABYLON
                is AddFactorSourceInput.Context.Recovery -> if (input.context.isOlympia) {
                    DeviceFactorSourceType.OLYMPIA
                } else {
                    DeviceFactorSourceType.BABYLON
                }
            }
        ).let {
            it.copy(
                hint = it.hint.copy(
                    label = state.value.name
                )
            )
        }

        return biometricsAuthenticateUseCase.asResult().then {
            mnemonicRepository.saveMnemonic(
                key = factorSource.id.asGeneral(),
                mnemonicWithPassphrase = args.mnemonicWithPassphrase
            )
        }.mapError {
            Timber.d(it)
            ProfileException.SecureStorageAccess
        }.then {
            preferencesManager.markFactorSourceBackedUp(factorSource.id.asGeneral())
            addFactorSource(factorSource.asGeneral())
        }
    }

    private suspend fun addFactorSource(factorSource: FactorSource) = sargonOsManager.callSafely(dispatcher) {
        addFactorSource(factorSource)
    }.mapCatching { added ->
        if (added) {
            factorSource.id
        } else {
            throw RadixWalletException.AddFactorSource.FactorSourceAlreadyInUse(
                factorSourceName = factorSource.name
            )
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
