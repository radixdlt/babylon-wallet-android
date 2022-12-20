package com.babylon.wallet.android.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.transaction.TransactionClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import models.transaction.TransactionManifest
import javax.inject.Inject

@HiltViewModel
class TransactionApprovalViewModel @Inject constructor(
    private val transactionClient: TransactionClient
) : ViewModel() {

    var state by mutableStateOf(TransactionUiState())
        private set

    fun approveTransaction(manifest: TransactionManifest) {
        viewModelScope.launch {
            val result = transactionClient.signAndSubmitTransaction(manifest)
            result.onValue { txId ->
            }
            result.onError {
            }
        }
    }
}

data class TransactionUiState(
    val manifest: TransactionManifest? = null
)
