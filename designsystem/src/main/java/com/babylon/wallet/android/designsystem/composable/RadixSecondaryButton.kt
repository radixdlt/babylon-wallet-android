package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.darken
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun RadixSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = RadixTheme.colors.gray4,
    contentColor: Color = RadixTheme.colors.gray1,
    shape: Shape = RadixTheme.shapes.roundedRectSmall,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    throttleClicks: Boolean = false,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var lastClickMs by remember { mutableStateOf(0L) }
    Button(
        modifier = modifier,
        onClick = {
            if (throttleClicks) {
                val now = System.currentTimeMillis()
                if (now - lastClickMs > 500) {
                    onClick()
                    lastClickMs = now
                }
            } else {
                onClick()
            }
        },
        shape = shape,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            contentColor = contentColor,
            containerColor = if (isPressed) containerColor.darken(0.1f) else containerColor,
            disabledContainerColor = RadixTheme.colors.gray4,
            disabledContentColor = RadixTheme.colors.gray3
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = RadixTheme.colors.white,
                    strokeWidth = 2.dp
                )
            } else {
                leadingContent?.invoke()
                Text(text = text, style = RadixTheme.typography.button)
                trailingContent?.invoke()
            }
        }
    }
}

@Preview
@Composable
fun RadixSecondaryButtonPreview() {
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
fun RadixSecondaryButtonWithIconPreview() {
    RadixWalletTheme {
        RadixSecondaryButton(
            text = "Secondary button",
            onClick = {},
            modifier = Modifier.size(200.dp, 50.dp),
            leadingContent = {
                Icon(painter = painterResource(id = R.drawable.ic_search), contentDescription = "")
            }
        )
    }
}

@Preview
@Composable
fun RadixSecondaryButtonDisabledPreview() {
    RadixWalletTheme {
        RadixSecondaryButton(
            text = "Secondary button",
            onClick = {},
            modifier = Modifier.size(200.dp, 50.dp),
            enabled = false
        )
    }
}
