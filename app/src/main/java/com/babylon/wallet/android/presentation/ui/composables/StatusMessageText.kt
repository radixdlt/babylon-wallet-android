package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.model.shared.StatusMessage

@Composable
fun StatusMessageText(
    modifier: Modifier = Modifier,
    message: StatusMessage
) {
    WarningText(
        modifier = modifier,
        text = message.message,
        textStyle = RadixTheme.typography.body2HighImportance,
        contentColor = message.type.color(),
        iconRes = message.type.iconRes()
    )
}

@Composable
private fun StatusMessage.Type.iconRes(): Int {
    return when (this) {
        StatusMessage.Type.SUCCESS -> DSR.ic_check_circle_outline
        StatusMessage.Type.WARNING -> DSR.ic_warning_error
        StatusMessage.Type.ERROR -> DSR.ic_warning_error
    }
}

@Composable
private fun StatusMessage.Type.color(): Color {
    return when (this) {
        StatusMessage.Type.SUCCESS -> RadixTheme.colors.ok
        StatusMessage.Type.WARNING -> RadixTheme.colors.warning
        StatusMessage.Type.ERROR -> RadixTheme.colors.error
    }
}
