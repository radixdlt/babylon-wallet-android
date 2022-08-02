package com.babylon.wallet.android.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.material.IconButton
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.extensions.ResponsiveText
import com.babylon.wallet.android.ui.theme.BabylonWalletTheme
import com.babylon.wallet.android.ui.theme.RadixLightCardBackground

@Composable
fun AccountCardView(
    onCardClick: () -> Unit,
    hashValue: String,
    accountName: String,
    accountValue: String,
    accountCurrency: String,
    onCopyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(25.dp, 0.dp, 25.dp, 0.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 8.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ),
        backgroundColor = RadixLightCardBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(27.dp, 18.dp, 27.dp, 18.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.h4,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, false)
                )
                Text(
                    text = "$accountCurrency$accountValue",
                    style = MaterialTheme.typography.h4
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResponsiveText(
                    modifier = Modifier.weight(1f, false),
                    text = hashValue,
                    style = MaterialTheme.typography.h6,
                    maxLines = 1
                )
                IconButton(
                    onClick = {
                        onCopyClick()
                    },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy),
                        ""
                    )
                }
            }
        }
    }
}

@Preview("default")
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun AccountCardPreview() {
    BabylonWalletTheme {
        AccountCardView(
            onCardClick = {},
            hashValue = "0x589e5cb09935F67c441AEe6AF46A365274a932e3",
            accountName = "My main account",
            accountValue = "19195",
            accountCurrency = "$",
            {},
        )
    }
}
