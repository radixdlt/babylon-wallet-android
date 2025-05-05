package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun TransactionRawManifestToggle(
    modifier: Modifier = Modifier,
    isToggleOn: Boolean,
    onRawManifestClick: () -> Unit
) {
    val icon = if (isToggleOn) {
        com.babylon.wallet.android.designsystem.R.drawable.ic_manifest_collapse
    } else {
        com.babylon.wallet.android.designsystem.R.drawable.ic_manifest_expand
    }
    IconButton(
        modifier = modifier
            .background(
                color = RadixTheme.colors.backgroundTertiary, // TODO Theme
                shape = RadixTheme.shapes.roundedRectSmall
            )
            .size(width = 50.dp, height = 40.dp),
        onClick = onRawManifestClick,
        colors = IconButtonColors(
            containerColor = RadixTheme.colors.backgroundTertiary,
            contentColor = RadixTheme.colors.icon,
            disabledContentColor = RadixTheme.colors.icon,
            disabledContainerColor = RadixTheme.colors.icon
        )
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "manifest expand"
        )
    }
}
