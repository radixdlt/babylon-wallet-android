package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.assets.FiatPrice
import com.babylon.wallet.android.domain.model.assets.SupportedCurrency
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import java.util.Currency
import java.util.Locale

@Composable
fun TotalBalanceView(
    modifier: Modifier = Modifier,
    fiatPrice: FiatPrice?,
    isLoading: Boolean,
    isHidden: Boolean,
    contentColor: Color = RadixTheme.colors.gray1,
    shimmeringColor: Color? = null,
    onShowHideClick: (isVisible: Boolean) -> Unit
) {
    if (isLoading) {
        TotalBalanceShimmering(
            modifier = modifier,
            shimmeringColor = shimmeringColor
        )
    } else {
        TotalBalanceContent(
            modifier = modifier,
            isHidden = isHidden,
            fiatPrice = fiatPrice,
            contentColor = contentColor,
            onShowHideClick = onShowHideClick
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
    modifier: Modifier,
    fiatPrice: FiatPrice?,
    isHidden: Boolean,
    contentColor: Color,
    onShowHideClick: (isVisible: Boolean) -> Unit
) {
    var isBalanceHidden by rememberSaveable { mutableStateOf(isHidden) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        val currencyCode = fiatPrice?.currency?.name ?: SupportedCurrency.USD.name
        if (isBalanceHidden) {
            Row(
                modifier = Modifier.padding(
                    top = RadixTheme.dimensions.paddingMedium,
                    bottom = RadixTheme.dimensions.paddingXSmall
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    text = Currency.getInstance(currencyCode).symbol,
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
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_hide),
                    contentDescription = "",
                    tint = RadixTheme.colors.gray3,
                    modifier = Modifier
                        .size(22.dp)
                        .fillMaxSize()
                        .align(Alignment.CenterVertically)
                        .clickable {
                            isBalanceHidden = !isBalanceHidden
                            onShowHideClick(!isBalanceHidden)
                        }
                )
            }
        } else {
            Text(
                modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingXXSmall),
                text = Currency.getInstance(currencyCode).symbol,
                style = RadixTheme.typography.title,
                color = if (fiatPrice != null) contentColor else RadixTheme.colors.gray3,
                fontSize = 20.sp,
            )
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = fiatPrice?.formattedWithoutCurrency
                    ?: "${Currency.getInstance(Locale.US).symbol}-", // "-" means an error occurred when calculating balance
                style = RadixTheme.typography.title,
                color = if (fiatPrice != null) contentColor else RadixTheme.colors.gray3
            )
            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_show),
                contentDescription = "",
                tint = RadixTheme.colors.gray3,
                modifier = Modifier
                    .size(22.dp)
                    .fillMaxSize()
                    .align(Alignment.CenterVertically)
                    .clickable {
                        isBalanceHidden = !isBalanceHidden
                        onShowHideClick(!isBalanceHidden)
                    }
            )
        }
    }
}

private const val NUMBER_OF_CIRCLES = 4

@Preview("default", showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Preview(showBackground = true)
@Composable
fun TotalBalancePreview() {
    RadixWalletTheme {
        TotalBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 246.6,
                currency = SupportedCurrency.USD
            ),
            isLoading = false,
            isHidden = false,
            onShowHideClick = {}
        )
    }
}

@Preview("default with long value", showBackground = true)
@Preview(showBackground = true)
@Composable
fun TotalBalanceWithLongValuePreview() {
    RadixWalletTheme {
        TotalBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 25747534664246.6,
                currency = SupportedCurrency.USD
            ),
            isLoading = false,
            isHidden = false,
            onShowHideClick = {}
        )
    }
}

@Preview("default", showBackground = true)
@Preview(showBackground = true)
@Composable
fun TotalBalanceErrorPreview() {
    RadixWalletTheme {
        TotalBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = null,
            isLoading = false,
            isHidden = false,
            onShowHideClick = {}
        )
    }
}

@Preview("default", showBackground = true)
@Preview(showBackground = true)
@Composable
fun TotalBalanceHiddenPreview() {
    RadixWalletTheme {
        TotalBalanceView(
            modifier = Modifier.fillMaxWidth(),
            fiatPrice = FiatPrice(
                price = 2246.6,
                currency = SupportedCurrency.USD
            ),
            isLoading = false,
            isHidden = true,
            onShowHideClick = {}
        )
    }
}
