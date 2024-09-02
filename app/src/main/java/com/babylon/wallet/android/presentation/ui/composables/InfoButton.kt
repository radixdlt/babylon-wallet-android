package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun InfoButton(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = RadixTheme.colors.blue2,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_info_outline),
            contentDescription = null,
            tint = color,
        )
        Text(
            text = text,
            style = RadixTheme.typography.body1StandaloneLink,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InfoButtonPreview() {
    RadixWalletPreviewTheme {
        InfoButton(text = "click here to see the info") {
        }
    }
}
