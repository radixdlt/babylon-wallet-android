package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.LocalBalanceVisibility
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import java.text.DecimalFormatSymbols

@Composable
fun TotalFiatBalanceView(
    modifier: Modifier = Modifier,
    fiatPrice: FiatPrice?,
    currency: SupportedCurrency,
    isLoading: Boolean,
    contentColor: Color = RadixTheme.colors.text,
    hiddenContentColor: Color = RadixTheme.colors.textTertiary,
    contentStyle: TextStyle = RadixTheme.typography.title,
    formattedContentStyle: TextStyle = contentStyle,
    shimmeringColor: Color? = null,
    onVisibilityToggle: (isVisible: Boolean) -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    if (isLoading) {
        TotalBalanceShimmering(
            modifier = modifier,
            shimmeringColor = shimmeringColor
        )
    } else {
        TotalBalanceContent(
            modifier = modifier,
            fiatPrice = fiatPrice,
            contentColor = contentColor,
            hiddenContentColor = hiddenContentColor,
            contentStyle = contentStyle,
            currency = currency,
            formattedContentStyle = formattedContentStyle,
            onVisibilityToggle = onVisibilityToggle,
            trailingContent = trailingContent
        )
    }
}

@Composable
private fun TotalBalanceShimmering(
    modifier: Modifier,
    shimmeringColor: Color? = null
) {
    Box(
        modifier = modifier
            .padding(top = RadixTheme.dimensions.paddingMedium)
            .height(30.dp)
            .fillMaxWidth(0.6f)
            .radixPlaceholder(
                visible = true,
                shape = RadixTheme.shapes.roundedRectSmall,
                color = shimmeringColor
            ),
    )
}

@Composable
private fun TotalBalanceContent(
    modifier: Modifier = Modifier,
    fiatPrice: FiatPrice?,
    currency: SupportedCurrency,
    contentColor: Color,
    hiddenContentColor: Color,
    contentStyle: TextStyle,
    formattedContentStyle: TextStyle,
    onVisibilityToggle: (isVisible: Boolean) -> Unit,
    trailingContent: (@Composable () -> Unit)?
) {
    val isPriceVisible = LocalBalanceVisibility.current
    val formatted = if (isPriceVisible) {
        remember(fiatPrice, currency) {
            fiatPrice?.defaultFormatted ?: currency.errorBalance
        }
    } else {
        remember(currency) {
            currency.hiddenBalance
        }
    }

    val annotatedFormat = if (formattedContentStyle != contentStyle) {
        buildAnnotatedString {
            val currencySymbol = fiatPrice?.currency?.symbol ?: currency.symbol
            val currencyStart = formatted.indexOf(currencySymbol)
            if (currencyStart != -1) {
                val currencyEnd = currencyStart + currencySymbol.length

                append(formatted)
                addStyle(style = formattedContentStyle.toSpanStyle(), start = currencyStart, end = currencyEnd)
            }

            val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator
            val decimalStart = formatted.indexOf(decimalSeparator)
            if (decimalStart != -1) {
                val decimalEnd = formatted.length
                addStyle(style = formattedContentStyle.toSpanStyle(), start = decimalStart, end = decimalEnd)
            }
        }
    } else {
        buildAnnotatedString { append(formatted) }
    }

    Row(
        modifier = modifier
            .clickable {
                onVisibilityToggle(!isPriceVisible)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val shouldShowContentColor = (fiatPrice != null && isPriceVisible && !fiatPrice.isZero)
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = annotatedFormat,
            style = contentStyle,
            color = if (shouldShowContentColor) contentColor else hiddenContentColor,
            maxLines = 1
        )

        trailingContent?.invoke()
    }
}

@Composable
fun TotalFiatBalanceViewToggle(
    modifier: Modifier = Modifier,
    onToggle: (isVisible: Boolean) -> Unit
) {
    val isPriceVisible = LocalBalanceVisibility.current
    Icon(
        painter = painterResource(
            id = if (isPriceVisible) {
                com.babylon.wallet.android.designsystem.R.drawable.ic_show
            } else {
                com.babylon.wallet.android.designsystem.R.drawable.ic_hide
            }
        ),
        contentDescription = "",
        tint = RadixTheme.colors.iconTertiary,
        modifier = modifier
            .padding(start = RadixTheme.dimensions.paddingSmall)
            .size(22.dp)
            .fillMaxSize()
            .clickable {
                onToggle(!isPriceVisible)
            }
    )
}

@Preview(showBackground = true)
@Composable
fun TotalFiatBalanceZeroPreview() {
    RadixWalletTheme {
        TotalFiatBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 0.toDecimal192(),
                currency = SupportedCurrency.USD
            ),
            currency = SupportedCurrency.USD,
            isLoading = false,
            onVisibilityToggle = {},
            trailingContent = {
                TotalFiatBalanceViewToggle(onToggle = {})
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TotalFiatBalancePreview() {
    RadixWalletTheme {
        TotalFiatBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 246.608903.toDecimal192(),
                currency = SupportedCurrency.USD
            ),
            currency = SupportedCurrency.USD,
            isLoading = false,
            onVisibilityToggle = {},
            trailingContent = {
                TotalFiatBalanceViewToggle(onToggle = {})
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TotalFiatBalanceWithValueLessThanOnePreview() {
    RadixWalletTheme {
        TotalFiatBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 0.608903.toDecimal192(),
                currency = SupportedCurrency.USD
            ),
            currency = SupportedCurrency.USD,
            isLoading = false,
            onVisibilityToggle = {},
            trailingContent = {
                TotalFiatBalanceViewToggle(onToggle = {})
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TotalFiatBalanceWithVerySmallValuePreview() {
    RadixWalletTheme {
        TotalFiatBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 0.0000000003.toDecimal192(),
                currency = SupportedCurrency.USD
            ),
            currency = SupportedCurrency.USD,
            isLoading = false,
            onVisibilityToggle = {},
            trailingContent = {
                TotalFiatBalanceViewToggle(onToggle = {})
            }
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun TotalFiatBalanceWithLongValuePreview() {
    RadixWalletTheme {
        TotalFiatBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 25747534664246.6.toDecimal192(),
                currency = SupportedCurrency.USD
            ),
            currency = SupportedCurrency.USD,
            isLoading = false,
            onVisibilityToggle = {},
            trailingContent = {
                TotalFiatBalanceViewToggle(onToggle = {})
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TotalFiatBalanceErrorPreview() {
    RadixWalletTheme {
        TotalFiatBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = null,
            currency = SupportedCurrency.USD,
            isLoading = false,
            onVisibilityToggle = {},
            trailingContent = {
                TotalFiatBalanceViewToggle(onToggle = {})
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TotalFiatBalanceHiddenPreview() {
    RadixWalletTheme {
        CompositionLocalProvider(value = LocalBalanceVisibility.provides(false)) {
            TotalFiatBalanceView(
                modifier = Modifier.fillMaxWidth(),
                fiatPrice = FiatPrice(
                    price = 2246.6.toDecimal192(),
                    currency = SupportedCurrency.USD
                ),
                currency = SupportedCurrency.USD,
                isLoading = false,
                onVisibilityToggle = {},
                trailingContent = {
                    TotalFiatBalanceViewToggle(onToggle = {})
                }
            )
        }
    }
}
