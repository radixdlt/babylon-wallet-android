package com.babylon.wallet.android.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.IncomingRequest
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.transaction.IncomingRequestHolder
import com.babylon.wallet.android.domain.transaction.TransactionClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionApprovalViewModel @Inject constructor(
    private val transactionClient: TransactionClient,
    private val incomingRequestHolder: IncomingRequestHolder
) : ViewModel() {

    var state by mutableStateOf(TransactionUiState())
        private set

    init {
        viewModelScope.launch {
            incomingRequestHolder.receive(viewModelScope) {
                if (it is IncomingRequest.TransactionWriteRequest) {
                    state = state.copy(manifest = it.transactionManifestData)
                    approveTransaction(it.transactionManifestData)
                }
            }
        }
    }

    fun approveTransaction(manifestData: TransactionManifestData) {
        viewModelScope.launch {
            val result = transactionClient.signAndSubmitTransaction(manifestData)
            result.onValue { txId ->
            }
            result.onError {
            }
        }
    }
}

data class TransactionUiState(
    val manifest: TransactionManifestData? = null,
)
