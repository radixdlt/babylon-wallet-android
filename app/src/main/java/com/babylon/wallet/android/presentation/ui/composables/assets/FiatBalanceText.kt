package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.assets.FiatPrice
import com.babylon.wallet.android.domain.model.assets.SupportedCurrency
import com.babylon.wallet.android.presentation.LocalBalanceVisibility
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import java.util.Currency
import java.util.Locale

@Composable
fun FiatBalanceText(
    modifier: Modifier = Modifier,
    fiatPriceFormatted: String?,
    currency: SupportedCurrency?,
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
    } else if (LocalBalanceVisibility.current) {
        Text(
            modifier = modifier,
            text = fiatPriceFormatted ?: "${Currency.getInstance(Locale.US).symbol}-",
            style = textStyle,
            color = RadixTheme.colors.gray2,
            maxLines = 1,
        )
    } else {
        FiatBalanceHidden(currency = currency)
    }
}

@Composable
fun FiatBalanceText(
    modifier: Modifier = Modifier,
    fiatPrice: FiatPrice?,
    isLoading: Boolean,
    textStyle: TextStyle = RadixTheme.typography.body2HighImportance
) {
    FiatBalanceText(
        modifier = modifier,
        fiatPriceFormatted = fiatPrice?.formatted,
        isLoading = isLoading,
        currency = fiatPrice?.currency,
        textStyle = textStyle
    )
}

@Composable
private fun FiatBalanceHidden(currency: SupportedCurrency?) {
    Row(
        modifier = Modifier.padding(
            top = RadixTheme.dimensions.paddingMedium,
            bottom = RadixTheme.dimensions.paddingXSmall
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Text(
            text = currency?.let {
                Currency.getInstance(it.code).symbol
            } ?: Currency.getInstance(Locale.US).symbol,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray3,
            fontSize = 20.sp
        )
        repeat(NUMBER_OF_CIRCLES) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = RadixTheme.colors.gray3,
                        shape = RadixTheme.shapes.circle
                    )
            )
        }
    }
}

private const val NUMBER_OF_CIRCLES = 4

@Preview(showBackground = true)
@Composable
fun FiatBalanceTextPreview() {
    RadixWalletTheme {
        FiatBalanceText(
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
            FiatBalanceText(
                modifier = Modifier,
                isLoading = false,
                fiatPrice = FiatPrice(price = 1879.32, SupportedCurrency.USD)
            )
        }
    }
}
