package com.babylon.wallet.android.presentation.dapp.completion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.babylon.wallet.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsCompletionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(savedStateHandle.get<String>(Screen.ARG_DAPP_NAME).orEmpty())
    val state: StateFlow<String> = _state
}
