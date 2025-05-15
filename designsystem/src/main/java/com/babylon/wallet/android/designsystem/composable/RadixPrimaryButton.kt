package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.darken
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixThemeConfig
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
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
            contentColor = White,
            containerColor = if (isPressed) {
                RadixTheme.colors.primaryButton.darken(0.1f)
            } else {
                RadixTheme.colors.primaryButton
            },
            disabledContainerColor = RadixTheme.colors.backgroundTertiary,
            disabledContentColor = RadixTheme.colors.textTertiary
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
                    color = White,
                    strokeWidth = 2.dp
                )
            } else {
                icon?.invoke()
                Text(text = text, style = RadixTheme.typography.body1Header)
            }
        }
    }
}

@Preview
@Composable
fun RadixPrimaryButtonPreviewLight() {
    RadixWalletTheme {
        Box(modifier = Modifier.background(RadixTheme.colors.background)) {
            RadixPrimaryButton(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth(),
                text = "Primary button",
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
fun RadixPrimaryButtonPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(modifier = Modifier.background(RadixTheme.colors.background)) {
            RadixPrimaryButton(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth(),
                text = "Primary button",
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
fun RadixPrimaryButtonLoadingPreviewLight() {
    RadixWalletTheme {
        Box(modifier = Modifier.background(RadixTheme.colors.background)) {
            RadixPrimaryButton(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth(),
                text = "Primary button",
                isLoading = true,
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
fun RadixPrimaryButtonLoadingPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(modifier = Modifier.background(RadixTheme.colors.background)) {
            RadixPrimaryButton(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth(),
                text = "Primary button",
                isLoading = true,
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
fun RadixPrimaryButtonWithIconPreviewLight() {
    RadixWalletTheme {
        Box(modifier = Modifier.background(RadixTheme.colors.background)) {
            RadixPrimaryButton(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth(),
                text = "Primary button",
                onClick = {},
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = ""
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun RadixPrimaryButtonWithIconPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(modifier = Modifier.background(RadixTheme.colors.background)) {
            RadixPrimaryButton(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth(),
                text = "Primary button",
                onClick = {},
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = ""
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun RadixPrimaryButtonDisabledPreviewLight() {
    RadixWalletTheme {
        Box(modifier = Modifier.background(RadixTheme.colors.background)) {
            RadixPrimaryButton(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth(),
                text = "Primary button",
                onClick = {},
                enabled = false
            )
        }
    }
}

@Preview
@Composable
fun RadixPrimaryButtonDisabledPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(modifier = Modifier.background(RadixTheme.colors.background)) {
            RadixPrimaryButton(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth(),
                text = "Primary button",
                onClick = {},
                enabled = false
            )
        }
    }
}
