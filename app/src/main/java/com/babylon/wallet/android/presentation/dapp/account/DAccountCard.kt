package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.darken
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.TruncatedAddressText

@Composable
fun AccountCard(
    accountName: String,
    hashValue: String,
    accountCurrency: String,
    accountValue: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = modifier.clickable { onCheckedChange(!checked) },
        shape = RadixTheme.shapes.roundedRectSmall,
        backgroundColor = RadixTheme.colors.defaultBackground.darken(0.2f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row {
                    Text(
                        modifier = Modifier.weight(1f, false),
                        text = accountName,
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        style = RadixTheme.typography.body2Header,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))
                    Text(
                        text = "$accountCurrency$accountValue",
                        maxLines = 1,
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2
                    )
                }
                TruncatedAddressText(
                    text = hashValue,
                    maxLines = 1,
                    style = RadixTheme.typography.body2Link,
                    color = RadixTheme.colors.gray2
                )
            }
            Spacer(modifier = Modifier.weight(0.1f))
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DAppAccountCardPreview() {
    RadixWalletTheme {
        AccountCard(
            accountName = "Account name",
            hashValue = "jf932j9f32o",
            accountCurrency = "$",
            accountValue = "50000",
            checked = true,
            onCheckedChange = {}
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f)
@Composable
fun DAppAccountCardLargeFontPreview() {
    RadixWalletTheme {
        AccountCard(
            accountName = "Account name",
            hashValue = "jf932j9f32o",
            accountCurrency = "$",
            accountValue = "50000",
            checked = true,
            onCheckedChange = {}
        )
    }
}
