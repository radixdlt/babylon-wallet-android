package com.babylon.wallet.android.presentation.dapp.custom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.presentation.ui.theme.RadixLightCardBackground

@Composable
fun DAppLoginAccountCard(
    accountName: String,
    name: String,
    emailAddress: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelectedChange: (Boolean) -> Unit
) {

    Card(
        modifier = modifier
            .padding(1.dp)
            .clickable {
                onSelectedChange(!selected)
            },
        shape = RoundedCornerShape(6.dp),
        backgroundColor = RadixLightCardBackground
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accountName,
                    textAlign = TextAlign.Start,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = name,
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = emailAddress,
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            RadioButton(
                selected = selected,
                onClick = { onSelectedChange(!selected) }
            )
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun DAppAccountCardPreview() {
    DAppLoginAccountCard(
        accountName = "My Main",
        name = "John Smith",
        emailAddress = "jsmith@gmail.com",
        selected = true,
        onSelectedChange = {}
    )
}
