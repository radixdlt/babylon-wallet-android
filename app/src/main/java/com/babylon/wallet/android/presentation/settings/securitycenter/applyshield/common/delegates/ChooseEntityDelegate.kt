package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.delegates

import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models.ChooseEntityUiState
import kotlinx.coroutines.flow.update
import rdx.works.core.mapWhen
import javax.inject.Inject

interface ChooseEntityDelegate<T> {

    fun onSelectAllToggleClick()

    fun onSelectItem(item: T)
}

class ChooseEntityDelegateImpl<T> @Inject constructor() : ViewModelDelegate<ChooseEntityUiState<T>>(), ChooseEntityDelegate<T> {

    override fun onSelectAllToggleClick() {
        setAllSelected(!_state.value.selectedAll)
    }

    override fun onSelectItem(item: T) {
        _state.update { state ->
            state.copy(
                items = state.items.mapWhen(
                    predicate = { it.data == item },
                    mutation = { it.copy(selected = !it.selected) }
                )
            )
        }
    }

    fun getSelectedItems(): List<T> = _state.value.items.filter { it.selected }.map { it.data }

    private fun setAllSelected(selected: Boolean) {
        _state.update { state ->
            state.copy(
                items = state.items.map { it.copy(selected = selected) }
            )
        }
    }
}
