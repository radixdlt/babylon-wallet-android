package com.babylon.wallet.android.presentation.dapp

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.data.transaction.FactorSourceInteractionState
import com.babylon.wallet.android.presentation.status.signing.SigningStatusBottomDialog

@Composable
fun SigningStateDialog(
    factorSourceInteractionState: FactorSourceInteractionState?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var signingStateDismissed by remember { mutableStateOf(false) }
    if (!signingStateDismissed && factorSourceInteractionState != null) {
        SigningStatusBottomDialog(
            modifier = modifier.fillMaxHeight(0.8f),
            onDismissDialogClick = {
                signingStateDismissed = true
                onDismiss()
            },
            factorSourceInteractionState = factorSourceInteractionState
        )
    }
}
