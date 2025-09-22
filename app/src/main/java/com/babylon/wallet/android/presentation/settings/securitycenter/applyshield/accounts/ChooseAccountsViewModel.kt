package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.delegates.ChooseEntityDelegate
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.delegates.ChooseEntityDelegateImpl
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models.ChooseEntityEvent
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models.ChooseEntityUiState
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.EntitySecurityState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val chooseEntityDelegate: ChooseEntityDelegateImpl<Account>
) : StateViewModel<ChooseEntityUiState<Account>>(),
    ChooseEntityDelegate<Account> by chooseEntityDelegate,
    OneOffEventHandler<ChooseEntityEvent> by OneOffEventHandlerImpl() {

    init {
        chooseEntityDelegate(viewModelScope, _state)
        initAccounts()
    }

    override fun initialState(): ChooseEntityUiState<Account> = ChooseEntityUiState(mustSelectOne = true)

    fun onContinueClick() {
        viewModelScope.launch {
            sendEvent(
                ChooseEntityEvent.EntitySelected(
                    address = chooseEntityDelegate.getSelectedItem()
                        .let { AddressOfAccountOrPersona.Account(it.address) }
                )
            )
        }
    }

    private fun initAccounts() {
        viewModelScope.launch {
            val profile = getProfileUseCase()
            val accounts = profile.activeAccountsOnCurrentNetwork.filter {
                it.securityState is EntitySecurityState.Unsecured
            }
            val personas = profile.activePersonasOnCurrentNetwork.filter {
                it.securityState is EntitySecurityState.Unsecured
            }
            _state.update { state ->
                state.copy(
                    items = accounts.map { Selectable(it) },
                    canSkip = personas.isNotEmpty()
                )
            }
        }
    }

    fun onSkipClick() {
        viewModelScope.launch { sendEvent(ChooseEntityEvent.Skip) }
    }
}
