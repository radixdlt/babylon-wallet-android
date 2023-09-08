package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun UnderlineTextButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        shape = RadixTheme.shapes.roundedRectSmall,
        colors = ButtonDefaults.textButtonColors(
            contentColor = RadixTheme.colors.blue1,
            disabledContentColor = RadixTheme.colors.gray3,
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            style = RadixTheme.typography.body1Link,
            textDecoration = TextDecoration.Underline
        )
    }
}
