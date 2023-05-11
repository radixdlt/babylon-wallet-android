package com.babylon.wallet.android.presentation.account.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.presentation.account.SelectedResource
import com.babylon.wallet.android.presentation.model.NftCollectionUiModel
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.applyImageAspectRatio
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl

@Composable
fun NonFungibleTokenBottomSheetDetails(
    nonFungibleResource: AccountWithResources.Resource.NonFungibleResource,
    id: String,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(id = R.string.empty),
            onBackClick = onCloseClick,
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Close
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            val painter = rememberAsyncImagePainter(
//                model = rememberImageUrl(
//                    fromUrl = nonFungibleResource.nftImage,
//                    size = ImageSize.LARGE
//                ),
//                placeholder = painterResource(id = R.drawable.img_placeholder),
//                error = painterResource(id = R.drawable.img_placeholder)
//            )
            Image(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_token),
                contentDescription = "Nft image",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
//                    .applyImageAspectRatio(painter = painter)
                    .clip(RadixTheme.shapes.roundedRectMedium)
                    .background(Color.Transparent, RadixTheme.shapes.roundedRectMedium)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            AssetMetadataRow(
                modifier = Modifier.fillMaxWidth(),
                key = stringResource(id = R.string.nft_id)
            ) {
                ActionableAddressView(
                    address = nonFungibleResource.globalId(id),
                    textStyle = RadixTheme.typography.body1Regular,
                    textColor = RadixTheme.colors.gray1
                )
            }
//            nonFungibleResource.nftsMetadata.forEach {
//                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
//                AssetMetadataRow(
//                    modifier = Modifier.fillMaxWidth(),
//                    key = it.first,
//                    value = it.second
//                )
//            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
