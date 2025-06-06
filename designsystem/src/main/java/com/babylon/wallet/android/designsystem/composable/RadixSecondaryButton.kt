package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.darken
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixThemeConfig
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.utils.ClickListenerUtils

@Composable
fun RadixSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = RadixTheme.colors.secondaryButton,
    contentColor: Color = RadixTheme.colors.text,
    shape: Shape = RadixTheme.shapes.roundedRectSmall,
    textStyle: TextStyle = RadixTheme.typography.body1Header,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    throttleClicks: Boolean = false,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val lastClickMs = remember { mutableLongStateOf(0L) }
    Button(
        modifier = modifier.heightIn(min = RadixTheme.dimensions.buttonDefaultHeight),
        onClick = {
            ClickListenerUtils.throttleOnClick(
                lastClickMs = lastClickMs,
                onClick = onClick,
                enabled = throttleClicks
            )
        },
        shape = shape,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            contentColor = contentColor,
            containerColor = if (isPressed) containerColor.darken(0.1f) else containerColor,
            disabledContainerColor = RadixTheme.colors.backgroundTertiary,
            disabledContentColor = RadixTheme.colors.textTertiary
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = RadixTheme.colors.icon,
                    strokeWidth = 2.dp
                )
            } else {
                leadingContent?.invoke()
                Text(text = text, style = textStyle)
                trailingContent?.invoke()
            }
        }
    }
}

@Preview
@Composable
fun RadixSecondaryButtonPreviewLight() {
    RadixWalletTheme {
        RadixSecondaryButton(
            text = "Secondary button",
            onClick = {},
            modifier = Modifier.size(200.dp, 50.dp)
        )
    }
}

@Preview
@Composable
fun RadixSecondaryButtonPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        RadixSecondaryButton(
            text = "Secondary button",
            onClick = {},
            modifier = Modifier.size(200.dp, 50.dp)
        )
    }
}

@Preview
@Composable
fun RadixSecondaryButtonWithIconPreviewLight() {
    RadixWalletTheme {
        RadixSecondaryButton(
            text = "Secondary Button",
            onClick = {},
            leadingContent = {
                Icon(painter = painterResource(id = R.drawable.ic_search), contentDescription = "")
            }
        )
    }
}

@Preview
@Composable
fun RadixSecondaryButtonWithIconPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        RadixSecondaryButton(
            text = "Secondary Button",
            onClick = {},
            leadingContent = {
                Icon(painter = painterResource(id = R.drawable.ic_search), contentDescription = "")
            }
        )
    }
}

@Preview
@Composable
fun RadixSecondaryButtonDisabledPreviewLight() {
    RadixWalletTheme {
        RadixSecondaryButton(
            text = "Secondary button",
            onClick = {},
            modifier = Modifier.size(200.dp, 50.dp),
            enabled = false
        )
    }
}

@Preview
@Composable
fun RadixSecondaryButtonDisabledPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        RadixSecondaryButton(
            text = "Secondary button",
            onClick = {},
            modifier = Modifier.size(200.dp, 50.dp),
            enabled = false
        )
    }
}
