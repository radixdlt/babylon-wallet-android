package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.confirmseedphrase

import com.babylon.wallet.android.data.repository.factors.DeviceFactorSourceAddingClient
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConfirmDeviceSeedPhraseViewModel @Inject constructor(
    private val deviceFactorSourceAddingClient: DeviceFactorSourceAddingClient
) : StateViewModel<ConfirmDeviceSeedPhraseViewModel.State>() {

    override fun initialState(): State = State()

    data class State(
        val isLoading: Boolean = false
    ) : UiState
}