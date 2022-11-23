package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun RadixPrimaryButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = RadixTheme.shapes.roundedRectSmall,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor = RadixTheme.colors.blue2,
            disabledContainerColor = RadixTheme.colors.gray4,
            disabledContentColor = RadixTheme.colors.gray3
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            Text(text = text, style = RadixTheme.typography.button)
        }
    }
}

@Preview
@Composable
fun RadixPrimaryButtonPreview() {
    BabylonWalletTheme {
        RadixPrimaryButton(modifier = Modifier.size(200.dp, 50.dp), text = "Primary button", onClick = {})
    }
}

@Preview
@Composable
fun RadixPrimaryButtonWithIconPreview() {
    BabylonWalletTheme {
        RadixPrimaryButton(modifier = Modifier.size(200.dp, 50.dp), text = "Primary button", onClick = {}, icon = {
            Icon(painter = painterResource(id = R.drawable.ic_search), contentDescription = "")
        })
    }
}

@Preview
@Composable
fun RadixPrimaryButtonDisabledPreview() {
    BabylonWalletTheme {
        RadixPrimaryButton(
            modifier = Modifier.size(200.dp, 50.dp),
            text = "Primary button",
            onClick = {},
            enabled = false
        )
    }
}