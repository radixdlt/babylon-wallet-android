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
import com.radixdlt.sargon.Decimal192
import rdx.works.core.domain.formatted

@Composable
fun TokenBalance(
    modifier: Modifier = Modifier,
    amount: Decimal192?,
    symbol: String,
    align: TextAlign = TextAlign.Center
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            modifier = modifier,
            text = buildAnnotatedString {
                if (amount != null) {
                    append(amount.formatted())
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
}
