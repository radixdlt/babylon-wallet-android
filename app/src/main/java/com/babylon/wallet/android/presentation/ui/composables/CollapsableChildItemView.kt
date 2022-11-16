package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.theme.RadixGrey2

@Suppress("UnstableCollections")
@Composable
fun CollapsableChildItemView(
    nftId: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    bottomCornersRounded: Boolean = false,
    nftMetadata: List<Pair<String, String>> = emptyList()
) {
    val bottomCorners = if (bottomCornersRounded) 8.dp else 0.dp
    Card(
        modifier = modifier.animateContentSize()
            .fillMaxWidth()
            .padding(20.dp, 0.dp, 20.dp, 1.dp),
        shape = RoundedCornerShape(0.dp, 0.dp, bottomCorners, bottomCorners),
        backgroundColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
        ) {
            if (imageUrl != null && imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = imageUrl,
                        placeholder = painterResource(id = R.drawable.img_placeholder),
                        error = painterResource(id = R.drawable.img_placeholder)
                    ),
                    contentDescription = "Nft image",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .heightIn(min = 180.dp, max = 240.dp)
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            Text(
                nftId,
                color = RadixGrey2,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
            )

            Spacer(modifier = Modifier.height(16.dp))

            nftMetadata.forEach { metadata ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        metadata.first,
                        color = RadixGrey2,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        metadata.second,
                        color = RadixGrey2,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        Divider()
    }
}

@Preview(showBackground = true)
@Composable
fun CollapsableChildItemPreview() {
    CollapsableChildItemView(
        nftId = "123",
        imageUrl = "url",
        nftMetadata = listOf(
            Pair("Type", "Devin Booker - Dunk"),
            Pair("Type", "Reggie Jackson - Jump Shot")
        )
    )
}
