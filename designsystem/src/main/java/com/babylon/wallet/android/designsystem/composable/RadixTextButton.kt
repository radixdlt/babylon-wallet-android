package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun RadixTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = RadixTheme.typography.body1Header,
    contentColor: Color = RadixTheme.colors.blue2,
    enabled: Boolean = true
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        shape = RadixTheme.shapes.roundedRectSmall,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = RadixTheme.colors.gray3,
        )
    ) {
        Text(text = text, style = textStyle)
    }
}

@Preview
@Composable
fun RadixTextButtonPreview() {
    RadixWalletTheme {
        RadixTextButton(text = "Text button", onClick = {}, modifier = Modifier.size(200.dp, 50.dp))
    }
}

@Preview
@Composable
fun RadixTextButtonDisabledPreview() {
    RadixWalletTheme {
        RadixTextButton(
            text = "Text button",
            onClick = {},
            modifier = Modifier.size(200.dp, 50.dp),
            enabled = false
        )
    }
}
