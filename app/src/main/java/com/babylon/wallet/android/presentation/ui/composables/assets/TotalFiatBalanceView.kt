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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.assets.FiatPrice
import com.babylon.wallet.android.domain.model.assets.SupportedCurrency
import com.babylon.wallet.android.presentation.LocalBalanceVisibility
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import timber.log.Timber
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

@Composable
fun TotalFiatBalanceView(
    modifier: Modifier = Modifier,
    fiatPrice: FiatPrice?,
    currency: SupportedCurrency,
    isLoading: Boolean,
    contentColor: Color = RadixTheme.colors.gray1,
    hiddenContentColor: Color = RadixTheme.colors.gray3,
    contentStyle: TextStyle = RadixTheme.typography.title,
    formattedContentStyle: TextStyle = contentStyle,
    shimmeringColor: Color? = null,
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
    trailingContent: (@Composable () -> Unit)?
)  {
    val isPriceVisible = LocalBalanceVisibility.current
    val formatted = if (isPriceVisible) {
        remember(fiatPrice, currency) {
            fiatPrice?.formatted ?: currency.errorBalance
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
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = annotatedFormat,
            style = contentStyle,
            color = if (fiatPrice != null && isPriceVisible) contentColor else hiddenContentColor,
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
        painter = painterResource(id = if (isPriceVisible) {
            com.babylon.wallet.android.designsystem.R.drawable.ic_show
        } else {
            com.babylon.wallet.android.designsystem.R.drawable.ic_hide
        }),
        contentDescription = "",
        tint = RadixTheme.colors.gray3,
        modifier = modifier
            .padding(start = RadixTheme.dimensions.paddingSmall)
            .size(22.dp)
            .fillMaxSize()
            .clickable {
                onToggle(!isPriceVisible)
            }
    )
}

@Preview("default", showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Preview(showBackground = true)
@Composable
fun TotalBalancePreview() {
    RadixWalletTheme {
        TotalFiatBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 246.6,
                currency = SupportedCurrency.USD
            ),
            currency = SupportedCurrency.USD,
            isLoading = false,
            trailingContent = {
                TotalFiatBalanceViewToggle(onToggle = {})
            }
        )
    }
}

@Preview("default with long value", showBackground = true)
@Preview(showBackground = true)
@Composable
fun TotalBalanceWithLongValuePreview() {
    RadixWalletTheme {
        TotalFiatBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 25747534664246.6,
                currency = SupportedCurrency.USD
            ),
            currency = SupportedCurrency.USD,
            isLoading = false,
            trailingContent = {
                TotalFiatBalanceViewToggle(onToggle = {})
            }
        )
    }
}

@Preview("default", showBackground = true)
@Preview(showBackground = true)
@Composable
fun TotalBalanceErrorPreview() {
    RadixWalletTheme {
        TotalFiatBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = null,
            currency = SupportedCurrency.USD,
            isLoading = false,
            trailingContent = {
                TotalFiatBalanceViewToggle(onToggle = {})
            }
        )
    }
}

@Preview("default", showBackground = true)
@Preview(showBackground = true)
@Composable
fun TotalBalanceHiddenPreview() {
    RadixWalletTheme {
        TotalFiatBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 2246.6,
                currency = SupportedCurrency.USD
            ),
            currency = SupportedCurrency.USD,
            isLoading = false,
            trailingContent = {
                TotalFiatBalanceViewToggle(onToggle = {})
            }
        )
    }
}
