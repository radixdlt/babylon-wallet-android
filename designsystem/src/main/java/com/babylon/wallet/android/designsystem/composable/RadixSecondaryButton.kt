package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.darken
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun RadixSecondaryButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = RadixTheme.colors.gray4,
    contentColor: Color = RadixTheme.colors.gray1,
    shape: Shape = RadixTheme.shapes.roundedRectSmall,
    icon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Button(
        modifier = modifier,
        onClick = onClick,
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
            icon?.invoke()
            Text(text = text, style = RadixTheme.typography.button)
        }
    }
}

@Preview
@Composable
fun RadixSecondaryButtonPreview() {
    BabylonWalletTheme {
        RadixSecondaryButton(modifier = Modifier.size(200.dp, 50.dp), text = "Secondary button", onClick = {})
    }
}

@Preview
@Composable
fun RadixSecondaryButtonWithIconPreview() {
    BabylonWalletTheme {
        RadixSecondaryButton(modifier = Modifier.size(200.dp, 50.dp), text = "Secondary button", onClick = {}, icon = {
            Icon(painter = painterResource(id = R.drawable.ic_search), contentDescription = "")
        })
    }
}

@Preview
@Composable
fun RadixSecondaryButtonDisabledPreview() {
    BabylonWalletTheme {
        RadixSecondaryButton(
            modifier = Modifier.size(200.dp, 50.dp),
            text = "Secondary button",
            onClick = {},
            enabled = false
        )
    }
}