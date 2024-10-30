package com.babylon.wallet.android.presentation.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("VariableNaming")
open class DataHolderViewModelDelegate<D, S : UiState> {

    lateinit var viewModelScope: CoroutineScope

    lateinit var data: MutableStateFlow<D>
    lateinit var _state: MutableStateFlow<S>

    operator fun invoke(
        scope: CoroutineScope,
        data: MutableStateFlow<D>,
        state: MutableStateFlow<S>
    ) {
        this.viewModelScope = scope
        this.data = data
        this._state = state
    }
}
