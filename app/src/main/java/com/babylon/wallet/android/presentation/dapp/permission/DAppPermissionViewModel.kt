package com.babylon.wallet.android.presentation.dapp.permission

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DAppPermissionViewModel @Inject constructor() : ViewModel() {

    var state by mutableStateOf(DAppPermissionUiState())
        private set
}

data class DAppPermissionUiState(
    val dappMetadata: DappMetadata? = null,
    val isOngoing: Boolean = false,
    val uiMessage: UiMessage? = null,
    val showProgress: Boolean = true
)
