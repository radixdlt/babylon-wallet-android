package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.model.TokenUi
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme

@Composable
fun TokenItemCard(
    tokenUi: TokenUi
) {
    Card(
        shape = RoundedCornerShape(10),
        modifier = Modifier.fillMaxWidth(),
        elevation = 16.dp,
        backgroundColor = MaterialTheme.colors.primary,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = tokenUi.iconUrl,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background)
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Text(
                modifier = Modifier.padding(start = 10.dp),
                text = tokenUi.tokenItemTitle,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Column {
                Text(
                    text = tokenUi.tokenValue ?: "empty token value",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = tokenUi.tokenQuantity,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview("default")
@Preview("large font", fontScale = 2f)
@Composable
fun TokenItemCardPreview() {
    BabylonWalletTheme {
        TokenItemCard(
            tokenUi = TokenUi(
                id = "id",
                name = "name",
                symbol = "symbol",
                tokenQuantity = "token quantity",
                tokenValue = "token value",
                iconUrl = "icon url"
            )
        )
    }
}
