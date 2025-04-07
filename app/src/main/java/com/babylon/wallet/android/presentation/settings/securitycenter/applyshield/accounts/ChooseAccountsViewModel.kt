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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
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

    override fun initialState(): ChooseEntityUiState<Account> = ChooseEntityUiState(mustSelectAtLeastOne = true)

    fun onContinueClick() {
        viewModelScope.launch {
            sendEvent(
                ChooseEntityEvent.EntitiesSelected(
                    addresses = chooseEntityDelegate.getSelectedItems().map { AddressOfAccountOrPersona.Account(it.address) }
                )
            )
        }
    }

    private fun initAccounts() {
        viewModelScope.launch {
            val accounts = getProfileUseCase().activeAccountsOnCurrentNetwork
            _state.update { state -> state.copy(items = accounts.map { Selectable(it) }) }
        }
    }

    fun onSkipClick() {
        viewModelScope.launch {
            sendEvent(
                ChooseEntityEvent.EntitiesSelected(
                    addresses = emptyList()
                )
            )
        }
    }
}
