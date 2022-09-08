package com.babylon.wallet.android.presentation.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.babylon.wallet.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CreateAccountConfirmationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _accountUiState: MutableStateFlow<Pair<String, String>> = MutableStateFlow(
        Pair(
            savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME).orEmpty(),
            savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID).orEmpty()
        )
    )
    val accountUiState = _accountUiState.asStateFlow()
}
