package com.babylon.wallet.android.presentation.account.composable

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NonFungibleTokenBottomSheetDetails(
    item: Resource.NonFungibleResource.Item?,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    nonFungibleResource: Resource.NonFungibleResource? = null,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(id = R.string.empty),
            onBackClick = onCloseClick,
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Close
        )
        if (item != null) {
            val painter = rememberAsyncImagePainter(
                model = rememberImageUrl(
                    fromUrl = item.imageUrl,
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
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                    .clip(RadixTheme.shapes.roundedRectMedium)
                    .background(Color.Transparent, RadixTheme.shapes.roundedRectMedium)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            AssetMetadataRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                key = stringResource(id = R.string.assetDetails_NFTDetails_id)
            ) {
                ActionableAddressView(
                    address = item.globalAddress,
                    textStyle = RadixTheme.typography.body1HighImportance,
                    textColor = RadixTheme.colors.gray1
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            item.nameMetadataItem?.name?.let { name ->
                Row(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.assetDetails_name),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray2
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = name,
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        if (nonFungibleResource != null) {
            if (item != null) {
                Divider(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = RadixTheme.dimensions.paddingDefault),
                    color = RadixTheme.colors.gray4
                )
            }
            GrayBackgroundWrapper(contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingXLarge)) {
                val placeholder =
                    rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb()))
                AsyncImage(
                    model = rememberImageUrl(
                        fromUrl = nonFungibleResource.iconUrl,
                        size = ImageSize.LARGE
                    ),
                    placeholder = placeholder,
                    fallback = placeholder,
                    error = placeholder,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(vertical = RadixTheme.dimensions.paddingDefault)
                        .size(104.dp)
                        .background(RadixTheme.colors.gray3, RadixTheme.shapes.circle)
                        .clip(RadixTheme.shapes.circle)
                )

                if (nonFungibleResource.description.isNotBlank()) {
                    Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    Text(
                        text = nonFungibleResource.description,
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
                AddressRow(
                    modifier = Modifier.fillMaxWidth(),
                    address = nonFungibleResource.resourceAddress
                )
                if (nonFungibleResource.name.isNotBlank()) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.assetDetails_name),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = nonFungibleResource.name,
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                nonFungibleResource.currentSupplyToDisplay?.let { currentSupply ->
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.assetDetails_currentSupply),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            modifier = Modifier
                                .padding(start = RadixTheme.dimensions.paddingDefault),
                            text = currentSupply.toString(),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray1,
                            textAlign = TextAlign.End
                        )
                    }
                }
                if (nonFungibleResource.resourceBehaviours.isNotEmpty()) {
                    Column {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = RadixTheme.dimensions.paddingDefault,
                                    bottom = RadixTheme.dimensions.paddingSmall
                                ),
                            text = stringResource(id = R.string.assetDetails_behavior),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray2
                        )
                        nonFungibleResource.resourceBehaviours.forEach { resourceBehaviour ->
                            Behaviour(
                                icon = resourceBehaviour.icon(),
                                name = resourceBehaviour.name()
                            )
                        }
                    }
                }

                if (nonFungibleResource.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.assetDetails_tags),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray2
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            nonFungibleResource.tags.forEach { tag ->
                                Tag(
                                    modifier = Modifier
                                        .padding(RadixTheme.dimensions.paddingXSmall)
                                        .border(
                                            width = 1.dp,
                                            color = RadixTheme.colors.gray4,
                                            shape = RadixTheme.shapes.roundedTag
                                        )
                                        .padding(RadixTheme.dimensions.paddingSmall),
                                    tag = tag
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
