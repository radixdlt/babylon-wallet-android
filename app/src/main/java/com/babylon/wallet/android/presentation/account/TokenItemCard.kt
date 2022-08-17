package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.model.TokenUi
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme

private const val CORNER_SHAPE_PERCENT = 10
private const val WEIGHT_OF_TOKEN_NAME = 0.4F
private const val WEIGHT_OF_TOKEN_VALUE = 0.6F

@Composable
fun TokenItemCard(
    tokenUi: TokenUi,
    isFirst: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(CORNER_SHAPE_PERCENT),
        modifier = if (isFirst) Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp) else Modifier.fillMaxWidth(),
        elevation = if (isFirst) 16.dp else 1.dp,
        backgroundColor = MaterialTheme.colors.primary,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = tokenUi.iconUrl,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background), // TODO will change icon
                    fallback = painterResource(id = R.drawable.ic_launcher_background) // TODO will change icon
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .weight(WEIGHT_OF_TOKEN_NAME),
                text = tokenUi.tokenItemTitle,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Column(Modifier.weight(WEIGHT_OF_TOKEN_VALUE)) {
                Text(
                    text = tokenUi.tokenQuantity,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tokenUi.tokenValue.orEmpty(),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

@Preview("default with long name and long values")
@Preview("large font with long name and long values", fontScale = 2f)
@Composable
fun TokenItemCardWithLongNameAndLongValuesPreview() {
    BabylonWalletTheme {
        TokenItemCard(
            tokenUi = TokenUi(
                id = "id",
                name = "a very long name that might cause troubles",
                symbol = "",
                tokenQuantity = "24986986408506858486358909573",
                tokenValue = "299238528240295320u9532",
                iconUrl = "icon url"
            )
        )
    }
}
