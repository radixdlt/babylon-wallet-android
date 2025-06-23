package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.utils.ClickListenerUtils

@Composable
fun RadixTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = RadixTheme.typography.body1Header,
    contentColor: Color = RadixTheme.colors.textButton,
    enabled: Boolean = true,
    isWithoutPadding: Boolean = false,
    fontSize: TextUnit? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    throttleClicks: Boolean = false,
    textAlign: TextAlign? = null
) {
    val lastClickMs = remember { mutableStateOf(0L) }

    TextButton(
        modifier = modifier,
        onClick = {
            ClickListenerUtils.throttleOnClick(
                lastClickMs = lastClickMs,
                onClick = onClick,
                enabled = throttleClicks
            )
        },
        shape = RadixTheme.shapes.roundedRectSmall,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = RadixTheme.colors.textTertiary,
        ),
        contentPadding = if (isWithoutPadding) {
            PaddingValues(0.dp)
        } else {
            ButtonDefaults.TextButtonContentPadding
        },
    ) {
        leadingIcon?.invoke()
        Text(
            text = text,
            style = textStyle,
            fontSize = fontSize ?: textStyle.fontSize,
            textAlign = textAlign
        )
        trailingIcon?.invoke()
    }
}

@Preview(showBackground = true)
@Composable
fun RadixTextButtonPreview() {
    RadixWalletTheme {
        RadixTextButton(text = "Text button", onClick = {}, modifier = Modifier.size(200.dp, 50.dp))
    }
}

@Preview(showBackground = true)
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
