package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.CountedAmount
import com.babylon.wallet.android.presentation.transaction.composables.CountedAmountSection

@Composable
fun TokenBalance(
    modifier: Modifier = Modifier,
    amount: CountedAmount?,
    symbol: String?,
    amountTextStyle: TextStyle = RadixTheme.typography.secondaryHeader
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        amount?.let {
            CountedAmountSection(
                amount = it,
//                symbol = symbol, todo
                amountTextStyle = amountTextStyle
            )
        }

        if (amount != null && symbol != null) {
            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
        }

        symbol?.let {
            Text(
                text = symbol,
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1
            )
        }
    }
}
