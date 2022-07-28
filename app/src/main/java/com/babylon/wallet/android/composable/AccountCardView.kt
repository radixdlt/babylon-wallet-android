package com.babylon.wallet.android.composable

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
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
        modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth()
                .padding(27.dp, 18.dp, 27.dp, 18.dp)
        ){
            Row() {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.h4
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = accountCurrency,
                    style = MaterialTheme.typography.h4
                )
                Text(
                    text = accountValue,
                    style = MaterialTheme.typography.h4
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.padding(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hashValue,
                    style = MaterialTheme.typography.h6
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