package com.babylon.wallet.android.presentation.account.settings.delete.success

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AccountAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DeletedAccountSuccessViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
): StateViewModel<DeletedAccountSuccessViewModel.State>() {

    override fun initialState(): State = State(
        deletedAccountAddress = DeletedAccountSuccessArgs(savedStateHandle).deletedAccountAddress
    )

    init {
        // TODO change profile
    }

    data class State(
        val deletedAccountAddress: AccountAddress
    ): UiState
}
