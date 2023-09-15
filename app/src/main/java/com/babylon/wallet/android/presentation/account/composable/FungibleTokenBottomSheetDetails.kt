package com.babylon.wallet.android.presentation.account.composable

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.ThumbnailRequestSize
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.icon
import com.babylon.wallet.android.presentation.ui.composables.name
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FungibleTokenBottomSheetDetails(
    fungible: Resource.FungibleResource,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = fungible.name,
            onBackClick = onCloseClick,
            modifier = Modifier.fillMaxWidth(),
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Close
        )
        Spacer(modifier = Modifier.height(22.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val placeholder = if (fungible.isXrd) {
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token)
            } else {
                rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb()))
            }
            AsyncImage(
                model = rememberImageUrl(fromUrl = fungible.iconUrl, size = ThumbnailRequestSize.LARGE),
                placeholder = placeholder,
                fallback = placeholder,
                error = placeholder,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(104.dp)
                    .background(RadixTheme.colors.gray3, RadixTheme.shapes.circle)
                    .clip(RadixTheme.shapes.circle)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            if (fungible.ownedAmount != BigDecimal.ZERO) {
                TokenBalance(fungibleResource = fungible)
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            if (fungible.description.isNotBlank()) {
                Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = fungible.description,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            AddressRow(
                modifier = Modifier.fillMaxWidth(),
                address = fungible.resourceAddress
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

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
                    text = fungible.currentSupplyToDisplay ?: stringResource(id = R.string.assetDetails_supplyUnkown),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            if (fungible.resourceBehaviours.isNotEmpty()) {
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
                    fungible.resourceBehaviours.forEach { resourceBehaviour ->
                        Behaviour(
                            icon = resourceBehaviour.icon(),
                            name = resourceBehaviour.name(fungible.isXrd)
                        )
                    }
                }
            }

            if (fungible.tags.isNotEmpty()) {
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
                        fungible.tags.forEach { tag ->
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

@Composable
fun Behaviour(
    modifier: Modifier = Modifier,
    icon: Painter,
    name: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RadixTheme.dimensions.paddingXSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = icon,
            contentDescription = "behaviour image"
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = name,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1,
        )

//        Icon(
//            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline),
//            contentDescription = null,
//            tint = RadixTheme.colors.gray3
//        )
    }
}

@Composable
fun Tag(
    modifier: Modifier = Modifier,
    tag: Resource.Tag
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = if (tag == Resource.Tag.Official) {
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_radix_tag)
            } else {
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_token_tag)
            },
            contentDescription = "tag image",
            tint = RadixTheme.colors.gray2
        )

        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            text = tag.name(),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2
        )
    }
}

@Preview
@Composable
fun FungibleTokenBottomSheetDetailsPreview() {
    RadixWalletTheme {
        FungibleTokenBottomSheetDetails(
            modifier = Modifier.background(RadixTheme.colors.defaultBackground),
            fungible = SampleDataProvider().sampleFungibleResources().first(),
            onCloseClick = {}
        )
    }
}
