package com.babylon.wallet.android.presentation.ui.composables.resultdialog.failed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.presentation.ui.composables.SomethingWentWrongDialog
import com.babylon.wallet.android.presentation.ui.composables.resultdialog.ResultBottomDialogViewModel

@Composable
fun FailedBottomDialog(
    modifier: Modifier = Modifier,
    viewModel: ResultBottomDialogViewModel,
    requestId: String,
    errorText: String,
    onBackPress: () -> Unit
) {
    val dismissHandler = {
        viewModel.incomingRequestHandled(requestId)
        onBackPress()
    }
    SomethingWentWrongDialog(
        modifier = modifier,
        onDismissRequest = dismissHandler,
        subtitle = errorText
    )
}
