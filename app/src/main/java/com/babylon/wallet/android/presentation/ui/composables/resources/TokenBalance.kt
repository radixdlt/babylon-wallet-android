package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun TokenBalance(
    modifier: Modifier = Modifier,
    amount: BigDecimal?,
    symbol: String,
    align: TextAlign = TextAlign.Center
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            if (amount != null) {
                append(amount.displayableQuantity())
                withStyle(style = RadixTheme.typography.header.toSpanStyle()) {
                    append(" $symbol")
                }
            }
        },
        style = RadixTheme.typography.title,
        color = RadixTheme.colors.gray1,
        textAlign = align
    )
}
