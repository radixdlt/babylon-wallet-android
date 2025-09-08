package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.personas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.delegates.ChooseEntityDelegate
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.delegates.ChooseEntityDelegateImpl
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models.ChooseEntityEvent
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models.ChooseEntityUiState
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class ChoosePersonasViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val chooseEntityDelegate: ChooseEntityDelegateImpl<Persona>
) : StateViewModel<ChooseEntityUiState<Persona>>(),
    ChooseEntityDelegate<Persona> by chooseEntityDelegate,
    OneOffEventHandler<ChooseEntityEvent> by OneOffEventHandlerImpl() {

    private val args = ChoosePersonasArgs(savedStateHandle)

    init {
        chooseEntityDelegate(viewModelScope, _state)
        initPersonas()
    }

    override fun initialState(): ChooseEntityUiState<Persona> = ChooseEntityUiState(mustSelectOne = args.mustSelectOne)

    fun onContinueClick() {
        viewModelScope.launch {
            sendEvent(
                ChooseEntityEvent.EntitySelected(
                    address = chooseEntityDelegate.getSelectedItem().let { AddressOfAccountOrPersona.Identity(it.address) }
                )
            )
        }
    }

    private fun initPersonas() {
        viewModelScope.launch {
            val personas = getProfileUseCase().activePersonasOnCurrentNetwork
            _state.update { state -> state.copy(items = personas.map { Selectable(it) }) }
        }
    }
}
