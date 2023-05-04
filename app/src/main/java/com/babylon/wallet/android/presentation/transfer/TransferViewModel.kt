package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<TransferUiState>() {

    internal val args = TransferArgs(savedStateHandle)

    override fun initialState(): TransferUiState = TransferUiState()

    init {
        viewModelScope.launch {
            getProfileUseCase.accountOnCurrentNetwork(args.accountId)?.let { account ->
                _state.update {
                    it.copy(
                        fromAccount = AccountItemUiModel(
                            address = account.address,
                            displayName = account.displayName,
                            appearanceID = account.appearanceID
                        )
                    )
                }
            }
        }
    }
}

data class TransferUiState(
    val fromAccount: AccountItemUiModel? = null
) : UiState
