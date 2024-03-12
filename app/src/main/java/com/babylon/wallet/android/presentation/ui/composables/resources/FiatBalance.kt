package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun FiatBalance(
    modifier: Modifier = Modifier,
    fiatPriceFormatted: String,
    style: TextStyle
) {
    Text(
        modifier = modifier,
        text = fiatPriceFormatted,
        color = RadixTheme.colors.gray2,
        style = style,
        maxLines = 1
    )
}