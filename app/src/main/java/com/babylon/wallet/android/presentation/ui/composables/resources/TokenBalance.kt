package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.utils.toLocaleNumberFormat
import rdx.works.core.displayableQuantity
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

@Composable
fun TokenBalance(
    modifier: Modifier = Modifier,
    amount: BigDecimal?,
    fiatValue: BigDecimal? = null,
    symbol: String,
    align: TextAlign = TextAlign.Center
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        fiatValue?.let { // TODO FiatBalanceText
            Text(
                text = fiatValue.let { Currency.getInstance(Locale.US).symbol + fiatValue.toLocaleNumberFormat() },
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2,
                maxLines = 1
            )
        }
    }
}
