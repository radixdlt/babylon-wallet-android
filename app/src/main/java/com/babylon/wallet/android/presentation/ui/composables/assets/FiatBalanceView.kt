package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.assets.FiatPrice
import com.babylon.wallet.android.domain.model.assets.SupportedCurrency
import com.babylon.wallet.android.presentation.LocalBalanceVisibility
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder

@Composable
fun FiatBalanceView(
    modifier: Modifier = Modifier,
    fiatPrice: FiatPrice,
    isLoading: Boolean,
    textStyle: TextStyle = RadixTheme.typography.body2HighImportance
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .padding(top = RadixTheme.dimensions.paddingXXSmall)
                .height(12.dp)
                .fillMaxWidth(0.2f)
                .radixPlaceholder(
                    visible = true,
                    shape = RadixTheme.shapes.roundedRectXSmall,
                )
        )
    } else {
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
}

@Preview(showBackground = true)
@Composable
fun FiatBalanceTextPreview() {
    RadixWalletTheme {
        FiatBalanceView(
            modifier = Modifier,
            isLoading = false,
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
                isLoading = false,
                fiatPrice = FiatPrice(price = 1879.32, SupportedCurrency.USD)
            )
        }
    }
}
