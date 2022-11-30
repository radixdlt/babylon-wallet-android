package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.designsystem.theme.RadixGrey2
import com.babylon.wallet.android.designsystem.theme.RadixLightCardBackground
import com.babylon.wallet.android.presentation.ui.composables.TruncatedAddressText

@Composable
fun AccountCard(
    accountName: String,
    hashValue: String,
    accountCurrency: String,
    accountValue: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = modifier.clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(8.dp),
        backgroundColor = RadixLightCardBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row {
                    Text(
                        modifier = Modifier.weight(1f, false),
                        text = accountName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        textAlign = TextAlign.Start,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "$accountCurrency$accountValue",
                        color = RadixGrey2,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W400,
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                }
                TruncatedAddressText(
                    text = hashValue,
                    fontSize = 14.sp,
                    color = RadixGrey2,
                    fontWeight = FontWeight.W400,
                    textAlign = TextAlign.Start,
                    maxLines = 1
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
    AccountCard(
        accountName = "Account name",
        hashValue = "jf932j9f32o",
        accountCurrency = "$",
        accountValue = "50000",
        checked = true,
        onCheckedChange = {}
    )
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f)
@Composable
fun DAppAccountCardLargeFontPreview() {
    AccountCard(
        accountName = "Account name",
        hashValue = "jf932j9f32o",
        accountCurrency = "$",
        accountValue = "50000",
        checked = true,
        onCheckedChange = {}
    )
}
