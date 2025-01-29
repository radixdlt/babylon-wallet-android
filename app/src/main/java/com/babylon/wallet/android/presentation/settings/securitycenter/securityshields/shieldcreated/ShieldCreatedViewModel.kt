package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldcreated

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.assets.GetAllAccountsXrdBalanceUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val REQUIRED_XRD_AMOUNT = 10 // TODO get from Sargon

@HiltViewModel
class ShieldCreatedViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val getAllAccountsXrdBalanceUseCase: GetAllAccountsXrdBalanceUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : StateViewModel<ShieldCreatedViewModel.State>() {

    private val args = ShieldCreatedArgs(savedStateHandle)

    init {
        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                val securityStructureOfFactorSourceIDs = requireNotNull(
                    securityStructuresOfFactorSourceIds().find { it.metadata.id == args.securityStructureId }
                )
                val hasInsufficientXrd = getAllAccountsXrdBalanceUseCase() < REQUIRED_XRD_AMOUNT.toDecimal192()

                _state.update { state ->
                    state.copy(
                        shieldName = securityStructureOfFactorSourceIDs.metadata.displayName.value,
                        hasInsufficientXrd = hasInsufficientXrd
                    )
                }
            }
        }
    }

    override fun initialState(): State = State()

    data class State(
        val shieldName: String = "",
        val hasInsufficientXrd: Boolean = false
    ) : UiState {

        val isButtonEnabled = !hasInsufficientXrd
    }
}
