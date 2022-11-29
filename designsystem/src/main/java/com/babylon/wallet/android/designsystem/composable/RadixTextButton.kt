package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun RadixTextButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit,
    textStyle: TextStyle = RadixTheme.typography.body1Header,
    enabled: Boolean = true
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        shape = RadixTheme.shapes.roundedRectSmall,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = RadixTheme.colors.blue2,
            disabledContentColor = RadixTheme.colors.gray3,
        )
    ) {
        Text(text = text, style = textStyle)
    }
}

@Preview
@Composable
fun RadixTextButtonPreview() {
    BabylonWalletTheme {
        RadixTextButton(modifier = Modifier.size(200.dp, 50.dp), text = "Text button", onClick = {})
    }
}

@Preview
@Composable
fun RadixTextButtonDisabledPreview() {
    BabylonWalletTheme {
        RadixTextButton(
            modifier = Modifier.size(200.dp, 50.dp),
            text = "Text button",
            onClick = {},
            enabled = false
        )
    }
}