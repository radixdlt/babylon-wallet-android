package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.transaction.composables.FungibleAmountSection

@Composable
fun TokenBalance(
    modifier: Modifier = Modifier,
    amount: FungibleAmount?,
    symbol: String?
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        amount?.let {
            FungibleAmountSection(
                fungibleAmount = it,
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
