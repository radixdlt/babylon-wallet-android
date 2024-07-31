package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.darken
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.utils.ClickListenerUtils

@Composable
fun RadixPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    isLoading: Boolean = false,
    throttleClicks: Boolean = true
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
        shape = RadixTheme.shapes.roundedRectSmall,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor = if (isPressed) RadixTheme.colors.blue2.darken(0.1f) else RadixTheme.colors.blue2,
            disabledContainerColor = RadixTheme.colors.gray4,
            disabledContentColor = RadixTheme.colors.gray3
        ),
        interactionSource = interactionSource
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
                icon?.invoke()
                Text(text = text, style = RadixTheme.typography.button)
            }
        }
    }
}

@Preview
@Composable
fun RadixPrimaryButtonPreview() {
    RadixWalletTheme {
        RadixPrimaryButton(text = "Primary button", onClick = {}, modifier = Modifier.size(200.dp, 50.dp))
    }
}

@Preview
@Composable
fun RadixPrimaryButtonWithIconPreview() {
    RadixWalletTheme {
        RadixPrimaryButton(text = "Primary button", onClick = {}, modifier = Modifier.size(200.dp, 50.dp), icon = {
            Icon(painter = painterResource(id = R.drawable.ic_search), contentDescription = "")
        })
    }
}

@Preview
@Composable
fun RadixPrimaryButtonDisabledPreview() {
    RadixWalletTheme {
        RadixPrimaryButton(
            text = "Primary button",
            onClick = {},
            modifier = Modifier.size(200.dp, 50.dp),
            enabled = false
        )
    }
}
