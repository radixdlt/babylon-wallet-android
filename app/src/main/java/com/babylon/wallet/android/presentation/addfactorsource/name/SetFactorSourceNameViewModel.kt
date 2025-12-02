package com.babylon.wallet.android.presentation.addfactorsource.name

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIntermediaryParams
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
import kotlinx.coroutines.withContext
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
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<SetFactorSourceNameViewModel.State>(),
    OneOffEventHandler<SetFactorSourceNameViewModel.Event> by OneOffEventHandlerImpl() {

    private val input = addFactorSourceIOHandler.getInput() as AddFactorSourceInput.WithKind
    private val params = checkNotNull(addFactorSourceIOHandler.getIntermediaryParams())

    private lateinit var addedFactorSourceId: FactorSourceId

    override fun initialState(): State = State(factorSourceKind = input.kind)

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
                Timber.d(throwable)

                val uiMessage = if (throwable is CommonException) {
                    when {
                        throwable is CommonException.SecureStorageAccessException &&
                            throwable.errorKind == SecureStorageAccessErrorKind.USER_CANCELLED -> null

                        else -> RadixWalletException.FactorSource.FactorSourceNotCreated
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

    private suspend fun saveFactorSource(): Result<FactorSourceId> = withContext(dispatcher) {
        when (params) {
            is AddFactorSourceIntermediaryParams.Mnemonic -> when (input.kind) {
                FactorSourceKind.DEVICE -> saveDeviceFactorSource(params)
                FactorSourceKind.ARCULUS_CARD -> runCatching {
                    FactorSource.ArculusCard.init(
                        mnemonicWithPassphrase = params.value,
                        name = state.value.name
                    )
                }.then { factorSource ->
                    saveFactorSource(factorSource)
                }

                FactorSourceKind.OFF_DEVICE_MNEMONIC -> runCatching {
                    FactorSource.OffDeviceMnemonic.init(
                        mnemonicWithPassphrase = params.value,
                        name = state.value.name
                    )
                }.then { factorSource ->
                    saveFactorSource(factorSource)
                }

                else -> error("Not yet supported")
            }

            is AddFactorSourceIntermediaryParams.Ledger -> runCatching {
                FactorSource.Ledger.init(
                    id = params.factorSourceId,
                    model = params.model,
                    name = state.value.name
                )
            }.then { factorSource ->
                saveFactorSource(factorSource)
            }
        }
    }

    private suspend fun saveDeviceFactorSource(
        params: AddFactorSourceIntermediaryParams.Mnemonic
    ): Result<FactorSourceId> {
        val factorSource = sargonOsManager.sargonOs.createDeviceFactorSource(
            mnemonicWithPassphrase = params.value,
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

        return mnemonicRepository.saveMnemonic(
            key = factorSource.id.asGeneral(),
            mnemonicWithPassphrase = params.value
        ).mapError {
            Timber.d(it)
            ProfileException.SecureStorageAccess
        }.then {
            preferencesManager.markFactorSourceBackedUp(factorSource.id.asGeneral())
            saveFactorSource(factorSource.asGeneral())
        }
    }

    private suspend fun saveFactorSource(factorSource: FactorSource) = sargonOsManager.callSafely(dispatcher) {
        addFactorSource(factorSource)
    }.mapCatching { added ->
        if (added) {
            factorSource.id
        } else {
            throw RadixWalletException.FactorSource.FactorSourceAlreadyInUse(
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
