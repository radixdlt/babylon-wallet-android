package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
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
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.utils.formatDecimalSeparator

private const val NUMBER_OF_CIRCLES = 6

@Composable
fun WalletBalanceView(
    currencySignValue: String,
    amount: String,
    hidden: Boolean,
    balanceClicked: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = RadixTheme.colors.gray1
) {
    var balanceHidden by rememberSaveable { mutableStateOf(hidden) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            text = currencySignValue,
            style = RadixTheme.typography.title,
            color = contentColor
        )

        if (balanceHidden) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)
            ) {
                repeat(NUMBER_OF_CIRCLES) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = RadixTheme.colors.gray3,
                                shape = RadixTheme.shapes.circle
                            )
                    )
                }
            }
        } else {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = amount.formatDecimalSeparator(),
                style = RadixTheme.typography.title,
                color = contentColor,
            )
        }

        IconButton(
            onClick = {
                balanceHidden = !balanceHidden
                balanceClicked()
            }
        ) {
            Icon(
                painter = if (balanceHidden) {
                    painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_show)
                } else {
                    painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_hide)
                },
                "",
                tint = RadixTheme.colors.gray3
            )
        }
    }
}

@Preview("default", showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Preview(showBackground = true)
@Composable
fun WalletBalancePreview() {
    BabylonWalletTheme {
        WalletBalanceView(
            currencySignValue = "$",
            amount = "1232",
            hidden = false,
            balanceClicked = {}
        )
    }
}

@Preview("default with long value", showBackground = true)
@Preview(showBackground = true)
@Composable
fun WalletBalanceWithLongValuePreview() {
    BabylonWalletTheme {
        WalletBalanceView(
            currencySignValue = "$",
            amount = "1879232.32",
            hidden = false,
            balanceClicked = {}
        )
    }
}

@Preview("default", showBackground = true)
@Preview(showBackground = true)
@Composable
fun WalletBalanceHiddenPreview() {
    BabylonWalletTheme {
        WalletBalanceView(
            currencySignValue = "$",
            amount = "1879.32",
            hidden = true,
            balanceClicked = {}
        )
    }
}
