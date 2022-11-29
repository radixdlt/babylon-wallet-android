package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Suppress("UnstableCollections")
@Composable
fun NftTokenDetailItem(
    nftId: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    bottomCornersRounded: Boolean = false,
    nftMetadata: List<Pair<String, String>> = emptyList()
) {
    val bottomCorners = if (bottomCornersRounded) 12.dp else 0.dp
    Card(
        modifier = modifier
            .animateContentSize()
            .fillMaxWidth()
            .padding(20.dp, 0.dp, 20.dp, 1.dp),
        shape = RoundedCornerShape(0.dp, 0.dp, bottomCorners, bottomCorners),
        backgroundColor = RadixTheme.colors.defaultBackground,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            verticalArrangement = spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            AsyncImage(
                model = imageUrl,
                placeholder = painterResource(id = R.drawable.img_placeholder),
                error = painterResource(id = R.drawable.img_placeholder),
                contentDescription = "Nft image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RadixTheme.shapes.roundedRectMedium)
                    .background(Color.Transparent, RadixTheme.shapes.roundedRectMedium)
            )
            Text(
                nftId,
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body2HighImportance
            )
            nftMetadata.forEach { metadata ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = SpaceBetween) {
                    Text(
                        modifier = Modifier.weight(0.35f),
                        text = metadata.first,
                        color = RadixTheme.colors.gray2,
                        style = RadixTheme.typography.body1Regular
                    )
                    Text(
                        modifier = Modifier.weight(0.65f),
                        text = metadata.second,
                        color = RadixTheme.colors.gray1,
                        style = RadixTheme.typography.body1HighImportance,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
        }
        Divider()
    }
}

@Preview(showBackground = true)
@Composable
fun CollapsableChildItemPreview() {
    BabylonWalletTheme {
        NftTokenDetailItem(
            nftId = "123",
            imageUrl = "url",
            nftMetadata = listOf(
                Pair("Type", "Devin Booker - Dunk"),
                Pair("Type", "Reggie Jackson - Jump Shot")
            )
        )
    }
}
