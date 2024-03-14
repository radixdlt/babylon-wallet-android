package com.babylon.wallet.android.presentation.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.assets.FiatPrice
import com.babylon.wallet.android.domain.model.assets.SupportedCurrency
import com.babylon.wallet.android.presentation.LocalBalanceVisibility
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import java.util.Currency
import java.util.Locale

@Composable
fun AccountBalanceLabel(
    modifier: Modifier = Modifier,
    fiatValue: FiatPrice?,
    isLoading: Boolean
) {
    if (isLoading) {
        AccountBalanceShimmering(modifier = modifier)
    } else {
        if (LocalBalanceVisibility.current) {
            AccountBalanceContent(
                modifier = modifier,
                fiatValue = fiatValue
            )
        } else {
            AccountBalanceHidden(modifier = modifier)
        }
    }
}

@Composable
private fun AccountBalanceContent(
    modifier: Modifier,
    fiatValue: FiatPrice?
) {
    val fiatValueText = fiatValue?.let {
        if (it.price == 0.0) {
            ""
        } else {
            it.formatted
        }
    } ?: "${Currency.getInstance(Locale.US).symbol}-" // "-" means an error occurred when calculating balance
    Text(
        modifier = modifier,
        text = fiatValueText,
        style = RadixTheme.typography.body1Header,
        maxLines = 1,
        color = if (fiatValue != null) RadixTheme.colors.white else RadixTheme.colors.gray3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AccountBalanceHidden(modifier: Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
    ) {
        Text(
            text = Currency.getInstance(Locale.US).symbol, // TODO to update when we support multiple currencies
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray3,
        )
        repeat(NUMBER_OF_CIRCLES) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = RadixTheme.colors.gray3,
                        shape = RadixTheme.shapes.circle
                    )
            )
        }
    }
}

@Composable
private fun AccountBalanceShimmering(modifier: Modifier) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth(0.5f)
                .radixPlaceholder(
                    visible = true,
                    color = RadixTheme.colors.defaultBackground.copy(alpha = 0.6f),
                    shape = RadixTheme.shapes.roundedRectSmall,
                ),
        )
    }
}

private const val NUMBER_OF_CIRCLES = 4

@Preview(showBackground = false)
@Composable
fun AccountBalanceLabelPreview() {
    RadixWalletTheme {
        AccountBalanceContent(
            modifier = Modifier.fillMaxWidth(),
            fiatValue = FiatPrice(price = 1879.32, currency = SupportedCurrency.USD),
        )
    }
}

@Preview(showBackground = false)
@Composable
fun AccountWithZeroBalanceLabelPreview() {
    RadixWalletTheme {
        AccountBalanceContent(
            modifier = Modifier.fillMaxWidth(),
            fiatValue = FiatPrice(price = 0.0, currency = SupportedCurrency.USD),
        )
    }
}

@Preview(showBackground = false)
@Composable
fun AccountBalanceLabelHiddenPreview() {
    RadixWalletTheme {
        CompositionLocalProvider(value = LocalBalanceVisibility.provides(false)) {
            AccountBalanceLabel(
                modifier = Modifier.fillMaxWidth(),
                fiatValue = FiatPrice(price = 1879.32, currency = SupportedCurrency.USD),
                isLoading = false
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun AccountBalanceLabelErrorPreview() {
    RadixWalletTheme {
        AccountBalanceContent(
            modifier = Modifier.fillMaxWidth(),
            fiatValue = null,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun AccountBalanceLabelLoadingPreview() {
    RadixWalletTheme {
        CompositionLocalProvider(value = LocalBalanceVisibility.provides(false)) {
            AccountBalanceLabel(
                modifier = Modifier.fillMaxWidth(),
                fiatValue = FiatPrice(price = 1879.32, currency = SupportedCurrency.USD),
                isLoading = true
            )
        }
    }
}
