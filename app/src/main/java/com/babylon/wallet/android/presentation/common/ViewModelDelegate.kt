package com.babylon.wallet.android.presentation.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("VariableNaming")
open class ViewModelDelegate<T : UiState> {

    lateinit var viewModelScope: CoroutineScope
    lateinit var _state: MutableStateFlow<T>

    open operator fun invoke(
        scope: CoroutineScope,
        state: MutableStateFlow<T>
    ) {
        this.viewModelScope = scope
        _state = state
    }
}
