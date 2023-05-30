package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NftTokenHeaderItem(
    nftImageUrl: String?,
    nftName: String?,
    nftsInCirculation: String?,
    nftsInPossession: String?,
    nftChildCount: Int,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    parentSectionClick: () -> Unit,
) {
    val bottomPadding = if (collapsed) RadixTheme.dimensions.paddingSmall else 0.dp
    val bottomCorners = if (collapsed) 12.dp else 0.dp
    val cardShape = RoundedCornerShape(12.dp, 12.dp, bottomCorners, bottomCorners)
    Box(
        modifier = modifier
            .padding(0.dp, 10.dp, 0.dp, bottomPadding)
    ) {
        if (collapsed) {
            if (nftChildCount > 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(113.dp)
                        .padding(20.dp, 10.dp, 20.dp, 0.dp),
                    shape = RadixTheme.shapes.roundedRectMedium,
                    backgroundColor = Color.White,
                    elevation = 4.dp,
                    content = {}
                )
            }
            if (nftChildCount >= 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(103.dp)
                        .padding(10.dp, 10.dp, 10.dp, 0.dp),
                    shape = RadixTheme.shapes.roundedRectMedium,
                    backgroundColor = Color.White,
                    elevation = 4.dp,
                    content = {}
                )
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(93.dp).clickable { parentSectionClick() },
            shape = cardShape,
            backgroundColor = Color.White,
            elevation = 4.dp,
            onClick = parentSectionClick
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = rememberImageUrl(fromUrl = nftImageUrl, size = ImageSize.SMALL),
                        placeholder = painterResource(id = R.drawable.img_placeholder),
                        error = painterResource(id = R.drawable.img_placeholder)
                    ),
                    contentDescription = "Nft icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RadixTheme.shapes.roundedRectSmall)
                )
                Column(verticalArrangement = Arrangement.Center) {
                    nftName?.let { name ->
                        if (name.isNotEmpty()) {
                            Text(
                                name,
                                style = RadixTheme.typography.secondaryHeader,
                                color = RadixTheme.colors.gray1,
                                maxLines = 2
                            )
                        }
                    }
                    Text(
                        "$nftsInPossession of $nftsInCirculation",
                        style = RadixTheme.typography.body2HighImportance,
                        color = RadixTheme.colors.gray2,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CollapsableParentItemPreview() {
    RadixWalletTheme {
        NftTokenHeaderItem(
            nftImageUrl = "url",
            nftName = "Rypto Punks",
            nftsInCirculation = "300,000",
            nftsInPossession = "1",
            nftChildCount = 3,
            collapsed = false
        ) { }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpandedParentItemPreview() {
    RadixWalletTheme {
        NftTokenHeaderItem(
            nftImageUrl = "url",
            nftName = "Rypto Punks",
            nftsInCirculation = "300,000",
            nftsInPossession = "1",
            nftChildCount = 3,
            collapsed = true
        ) { }
    }
}

@Preview(fontScale = 2f, showBackground = true)
@Composable
fun CollapsableParentItemWithLargeFontPreview() {
    RadixWalletTheme {
        NftTokenHeaderItem(
            nftImageUrl = "url",
            nftName = "Rypto Punks",
            nftsInCirculation = "300,000",
            nftsInPossession = "1",
            nftChildCount = 3,
            collapsed = false
        ) { }
    }
}
