package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickeys

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessArculusFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessDeviceFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessLedgerHardwareWalletFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessOffDeviceMnemonicFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessPasswordFactorSourceUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceDelegate
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcePurpose
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.DerivationPurpose
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.toUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class DerivePublicKeysViewModel @Inject constructor(
    private val accessFactorSourcesIOHandler: AccessFactorSourcesIOHandler,
    private val accessDeviceFactorSource: AccessDeviceFactorSourceUseCase,
    private val accessLedgerHardwareWalletFactorSource: AccessLedgerHardwareWalletFactorSourceUseCase,
    private val accessOffDeviceMnemonicFactorSource: AccessOffDeviceMnemonicFactorSourceUseCase,
    private val accessArculusFactorSourceUseCase: AccessArculusFactorSourceUseCase,
    private val accessPasswordFactorSourceUseCase: AccessPasswordFactorSourceUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    getProfileUseCase: GetProfileUseCase,
) : StateViewModel<DerivePublicKeysViewModel.State>(),
    OneOffEventHandler<DerivePublicKeysViewModel.Event> by OneOffEventHandlerImpl() {

    private val proxyInput = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToDerivePublicKeys

    private val accessDelegate = AccessFactorSourceDelegate(
        viewModelScope = viewModelScope,
        id = proxyInput.request.factorSourceId.asGeneral(),
        getProfileUseCase = getProfileUseCase,
        accessOffDeviceMnemonicFactorSource = accessOffDeviceMnemonicFactorSource,
        defaultDispatcher = defaultDispatcher,
        onAccessCallback = this::onAccess,
        onDismissCallback = this::onDismissCallback,
        onFailCallback = this::onDismissCallback
    )

    override fun initialState(): State = State(
        purpose = when (proxyInput.purpose) {
            DerivationPurpose.ACCOUNT_RECOVERY -> AccessFactorSourcePurpose.DerivingAccounts
            else -> AccessFactorSourcePurpose.UpdatingFactorConfig
        },
        accessState = accessDelegate.state.value
    )

    init {
        accessDelegate
            .state
            .onEach { accessState ->
                _state.update { it.copy(accessState = accessState) }
            }
            .launchIn(viewModelScope)
    }

    fun onDismiss() = accessDelegate.onDismiss()

    fun onSeedPhraseWordChanged(wordIndex: Int, word: String) = accessDelegate.onSeedPhraseWordChanged(wordIndex, word)

    fun onPasswordTyped(password: String) = accessDelegate.onPasswordTyped(password)

    fun onRetry() = accessDelegate.onRetry()

    fun onMessageShown() = accessDelegate.onMessageShown()

    fun onInputConfirmed() = accessDelegate.onInputConfirmed()

    private suspend fun onAccess(factorSource: FactorSource): Result<Unit> = when (factorSource) {
        is FactorSource.Device -> accessDeviceFactorSource.derivePublicKeys(
            factorSource = factorSource,
            input = proxyInput.request
        )
        is FactorSource.Ledger -> accessLedgerHardwareWalletFactorSource.derivePublicKeys(
            factorSource = factorSource,
            input = proxyInput.request
        )
        is FactorSource.ArculusCard -> accessArculusFactorSourceUseCase.derivePublicKeys(
            factorSource = factorSource,
            input = proxyInput.request
        )
        is FactorSource.OffDeviceMnemonic -> accessOffDeviceMnemonicFactorSource.derivePublicKeys(
            factorSource = factorSource,
            input = proxyInput.request
        )
        is FactorSource.Password -> accessPasswordFactorSourceUseCase.derivePublicKeys(
            factorSource = factorSource,
            input = proxyInput.request
        )
    }.mapCatching { factorInstances ->
        finishWithSuccess(factorInstances)
    }.toUnit()

    private suspend fun onDismissCallback() {
        // end the signing process and return the output (error)
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.DerivedPublicKeys.Rejected)
    }

    private suspend fun finishWithSuccess(factorInstances: List<HierarchicalDeterministicFactorInstance>) {
        sendEvent(Event.Completed)
        accessFactorSourcesIOHandler.setOutput(
            AccessFactorSourcesOutput.DerivedPublicKeys.Success(
                factorSourceId = proxyInput.request.factorSourceId,
                factorInstances = factorInstances
            )
        )
    }

    data class State(
        val purpose: AccessFactorSourcePurpose,
        val accessState: AccessFactorSourceDelegate.State
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}
