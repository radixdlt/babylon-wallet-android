package com.babylon.wallet.android.presentation.dapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.presentation.status.signing.SigningStatusBottomDialog

@Composable
fun SigningStateDialog(
    interactionState: InteractionState?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    interactionState?.let {
        SigningStatusBottomDialog(
            modifier = modifier,
            onDismissDialogClick = onDismiss,
            interactionState = it
        )
    }
}
