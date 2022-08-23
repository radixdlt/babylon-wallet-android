package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme
import com.babylon.wallet.android.presentation.ui.theme.RadixGrey2
import com.babylon.wallet.android.utils.formatDecimalSeparator

private const val NUMBER_OF_CIRCLES = 6

@Composable
fun WalletBalanceView(
    currencySignValue: String,
    amount: String,
    hidden: Boolean,
    balanceClicked: () -> Unit
) {
    var balanceHidden by rememberSaveable { mutableStateOf(hidden) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            text = currencySignValue,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        if (balanceHidden) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(NUMBER_OF_CIRCLES) {
                    Canvas(
                        modifier = Modifier
                            .size(size = 20.dp)
                            .padding(4.dp)
                    ) {
                        drawCircle(
                            color = Color.LightGray
                        )
                    }
                }
            }
        } else {
            Text(
                text = amount.formatDecimalSeparator(),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        IconButton(
            onClick = {
                balanceHidden = !balanceHidden
                balanceClicked()
            }
        ) {
            Icon(
                imageVector = if (balanceHidden)
                    ImageVector.vectorResource(id = R.drawable.ic_eye_closed)
                else
                    ImageVector.vectorResource(id = R.drawable.ic_eye_open),
                "",
                tint = RadixGrey2
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
@Preview("large font with long value", fontScale = 2f, showBackground = true)
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
@Preview("large font", fontScale = 2f, showBackground = true)
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
