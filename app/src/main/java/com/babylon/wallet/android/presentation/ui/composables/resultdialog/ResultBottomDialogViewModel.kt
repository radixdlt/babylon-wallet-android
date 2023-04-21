package com.babylon.wallet.android.presentation.ui.composables.resultdialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultBottomDialogViewModel @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository
) : ViewModel() {

    fun incomingRequestHandled(requestId: String) {
        viewModelScope.launch {
            incomingRequestRepository.requestHandled(requestId)
        }
    }
}
