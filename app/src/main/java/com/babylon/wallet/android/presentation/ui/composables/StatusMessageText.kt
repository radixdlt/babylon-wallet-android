package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage

@Composable
fun StatusMessageText(
    message: StatusMessage
) {
    WarningText(
        text = AnnotatedString(message.message),
        textStyle = RadixTheme.typography.body2HighImportance,
        contentColor = message.type.color(),
        iconRes = message.type.iconRes()
    )
}

@Composable
private fun StatusMessage.Type.iconRes(): Int {
    return when (this) {
        StatusMessage.Type.SUCCESS -> com.babylon.wallet.android.designsystem.R.drawable.ic_check_circle_outline
        StatusMessage.Type.WARNING -> com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
        StatusMessage.Type.ERROR -> com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
    }
}

@Composable
private fun StatusMessage.Type.color(): Color {
    return when (this) {
        StatusMessage.Type.SUCCESS -> RadixTheme.colors.green1
        StatusMessage.Type.WARNING -> RadixTheme.colors.orange1
        StatusMessage.Type.ERROR -> RadixTheme.colors.red1
    }
}
