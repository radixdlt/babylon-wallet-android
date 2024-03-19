package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.LocalBalanceVisibility
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency

@Composable
fun FiatBalanceView(
    modifier: Modifier = Modifier,
    fiatPrice: FiatPrice,
    textStyle: TextStyle = RadixTheme.typography.body2HighImportance
) {
    Text(
        modifier = modifier,
        text = if (LocalBalanceVisibility.current) {
            remember(fiatPrice) {
                fiatPrice.formatted
            }
        } else {
            remember(fiatPrice) {
                fiatPrice.currency.hiddenBalance
            }
        },
        style = textStyle,
        color = RadixTheme.colors.gray2,
        maxLines = 1,
    )
}

@Preview(showBackground = true)
@Composable
fun FiatBalanceTextPreview() {
    RadixWalletTheme {
        FiatBalanceView(
            modifier = Modifier,
            fiatPrice = FiatPrice(price = 1879.32, SupportedCurrency.USD)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FiatBalanceTextHiddenPreview() {
    RadixWalletTheme {
        CompositionLocalProvider(value = LocalBalanceVisibility.provides(false)) {
            FiatBalanceView(
                modifier = Modifier,
                fiatPrice = FiatPrice(price = 1879.32, SupportedCurrency.USD)
            )
        }
    }
}
