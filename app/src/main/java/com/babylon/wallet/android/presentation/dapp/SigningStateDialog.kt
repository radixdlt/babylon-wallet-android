package com.babylon.wallet.android.presentation.dapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.babylon.wallet.android.data.transaction.SigningState
import com.babylon.wallet.android.presentation.ui.composables.resultdialog.signing.SigningStatusBottomDialog

@Composable
fun SigningStateDialog(signingState: SigningState?) {
    var signingStateDismissed by remember { mutableStateOf(false) }
    if (!signingStateDismissed && signingState != null) {
        SigningStatusBottomDialog(onDismissDialogClick = {
            signingStateDismissed = true
        }, signingState = signingState)
    }
}
