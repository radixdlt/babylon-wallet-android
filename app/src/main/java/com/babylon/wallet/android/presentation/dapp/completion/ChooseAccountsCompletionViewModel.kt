package com.babylon.wallet.android.presentation.dapp.completion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.babylon.wallet.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsCompletionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var dAppName by mutableStateOf(savedStateHandle.get<String>(Screen.ARG_DAPP_NAME).orEmpty())
        private set
}
