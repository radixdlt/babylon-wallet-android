package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.delegates

import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models.ChooseEntityUiState
import kotlinx.coroutines.flow.update
import rdx.works.core.mapWhen
import javax.inject.Inject

interface ChooseEntityDelegate<T> {

    fun onSelectItem(item: T)
}

class ChooseEntityDelegateImpl<T> @Inject constructor() : ViewModelDelegate<ChooseEntityUiState<T>>(), ChooseEntityDelegate<T> {

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

    fun getSelectedItem(): T = _state.value.items.first { it.selected }.data
}
