package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.babylon.wallet.android.domain.model.Resource

@OptIn(ExperimentalMaterialApi::class)
@Suppress("UnstableCollections")
@Composable
fun NftTokenDetailItem(
    item: Resource.NonFungibleResource.Item,
    modifier: Modifier = Modifier,
    bottomCornersRounded: Boolean = false,
    onItemClicked: () -> Unit
) {
    val bottomCorners by animateDpAsState(targetValue = if (bottomCornersRounded) 12.dp else 0.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 1.dp),
        shape = RoundedCornerShape(0.dp, 0.dp, bottomCorners, bottomCorners),
        backgroundColor = RadixTheme.colors.defaultBackground,
        elevation = 4.dp,
        onClick = {
            onItemClicked()
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            verticalArrangement = spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            if (item.imageUrl != null) {
                val painter = rememberAsyncImagePainter(
                    model = rememberImageUrl(
                        fromUrl = item.imageUrl.toString(),
                        size = ImageSize.LARGE
                    ),
                    placeholder = painterResource(id = R.drawable.img_placeholder),
                    error = painterResource(id = R.drawable.img_placeholder)
                )
                Image(
                    painter = painter,
                    contentDescription = "Nft image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .applyImageAspectRatio(painter = painter)
                        .clip(RadixTheme.shapes.roundedRectMedium)
                        .background(Color.Transparent, RadixTheme.shapes.roundedRectMedium)
                )
            }

            Text(
                item.localId,
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body2HighImportance
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
        Divider()
    }
}

@Preview(showBackground = true)
@Composable
fun CollapsableChildItemPreview() {
    RadixWalletTheme {
        NftTokenDetailItem(
            item = Resource.NonFungibleResource.Item(
                collectionAddress = "resource_rdx_abcd",
                localId = "#12",
                iconMetadataItem = null
            ),
            onItemClicked = { }
        )
    }
}
