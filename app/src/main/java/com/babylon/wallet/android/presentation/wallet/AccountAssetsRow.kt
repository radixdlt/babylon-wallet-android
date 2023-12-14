package com.babylon.wallet.android.presentation.wallet

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import java.math.BigDecimal

@Composable
fun AccountAssetsRow(
    modifier: Modifier = Modifier,
    assets: Assets?,
    isLoading: Boolean,
    iconSize: Dp = 30.dp,
    bordersSize: Dp = 1.dp,
    maxVisibleFungibles: Int = 5
) {
    AssetsContent(
        modifier = modifier
            .fillMaxWidth()
            .height(height = iconSize + bordersSize * 2)
            .placeholder(
                visible = isLoading,
                color = RadixTheme.colors.defaultBackground.copy(alpha = 0.6f),
                shape = RadixTheme.shapes.roundedRectSmall,
                highlight = PlaceholderHighlight.shimmer(
                    highlightColor = White
                ),
                placeholderFadeTransitionSpec = { tween() },
                contentFadeTransitionSpec = { tween() }
            ),
        assets = assets ?: Assets(),
        iconSize = iconSize,
        bordersSize = bordersSize,
        maxVisibleFungibles = maxVisibleFungibles
    )
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun AssetsContent(
    modifier: Modifier = Modifier,
    assets: Assets,
    iconSize: Dp,
    bordersSize: Dp,
    maxVisibleFungibles: Int,
    iconsOverlap: Dp = 10.dp
) {
    ConstraintLayout(
        modifier = modifier
    ) {
        val (visibleFungibles, remainingFungiblesCount) = remember(assets.ownedFungibles) {
            val all = assets.ownedFungibles
            all.take(maxVisibleFungibles) to (all.size - maxVisibleFungibles).coerceAtLeast(minimumValue = 0)
        }
        val nftsCount = remember(assets.nonFungibles) { assets.nonFungiblesSize() }
        val poolUnitCount = remember(assets.poolUnits, assets.validatorsWithStakes) {
            assets.poolUnitsSize()
        }

        val fungibleRefs = visibleFungibles.map { createRef() }
        val fungibleCounterBoxRef = if (remainingFungiblesCount > 0) createRef() else null
        val nftsRowRef = if (nftsCount > 0) createRef() else null
        val poolUnitRowRef = if (poolUnitCount > 0) createRef() else null

        visibleFungibles.forEachIndexed { index, fungible ->
            Thumbnail.Fungible(
                modifier = Modifier
                    .constrainAs(fungibleRefs[index]) {
                        linkTo(top = parent.top, bottom = parent.bottom)
                        height = Dimension.value(iconSize)
                        width = Dimension.value(iconSize)

                        if (index == 0) {
                            start.linkTo(parent.start)
                        } else {
                            val prevRef = fungibleRefs[index - 1]
                            start.linkTo(prevRef.start, margin = iconSize - iconsOverlap)
                        }
                    }
                    .zIndex(visibleFungibles.size - index.toFloat())
                    .border(
                        width = bordersSize,
                        color = RadixTheme.colors.white.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .padding(bordersSize),
                token = fungible
            )
        }

        if (fungibleCounterBoxRef != null) {
            CounterBox(
                modifier = Modifier.constrainAs(fungibleCounterBoxRef) {
                    start.linkTo(fungibleRefs.last().start)
                    linkTo(top = parent.top, bottom = parent.bottom)
                    height = Dimension.value(iconSize)
                },
                text = "+$remainingFungiblesCount",
                contentPadding = PaddingValues(
                    start = iconSize + RadixTheme.dimensions.paddingXSmall,
                    end = RadixTheme.dimensions.paddingSmall
                )
            )
        }

        if (nftsRowRef != null) {
            val fungibleRef = fungibleCounterBoxRef ?: fungibleRefs.lastOrNull()

            Box(
                modifier = Modifier.constrainAs(nftsRowRef) {
                    start.linkTo(
                        anchor = fungibleRef?.end ?: parent.start,
                        margin = if (fungibleRef != null) 12.dp else 0.dp
                    )
                }
            ) {
                CounterBox(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .height(iconSize),
                    text = "${assets.nonFungiblesSize()}",
                    contentPadding = PaddingValues(
                        start = iconSize + RadixTheme.dimensions.paddingSmall,
                        end = RadixTheme.dimensions.paddingSmall
                    )
                )

                Image(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(iconSize + bordersSize * 2)
                        .border(
                            width = bordersSize,
                            color = RadixTheme.colors.white.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(9.dp)
                        )
                        .padding(bordersSize)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(White, White.copy(alpha = 0.73f))
                            ),
                            shape = RadixTheme.shapes.roundedRectSmall
                        )
                        .padding(4.dp),
                    painter = painterResource(id = R.drawable.ic_nfts),
                    contentDescription = null
                )
            }
        }

        if (poolUnitRowRef != null) {
            val fungibleRef = fungibleCounterBoxRef ?: fungibleRefs.lastOrNull()
            Box(
                modifier = Modifier.constrainAs(poolUnitRowRef) {
                    val previousElementRef = nftsRowRef ?: fungibleRef
                    start.linkTo(
                        anchor = (previousElementRef)?.end ?: parent.start,
                        margin = if (previousElementRef != null) 12.dp else 0.dp
                    )
                }
            ) {
                CounterBox(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .height(iconSize),
                    text = assets.poolUnitsSize().toString(),
                    contentPadding = PaddingValues(
                        start = iconSize + RadixTheme.dimensions.paddingSmall,
                        end = RadixTheme.dimensions.paddingSmall
                    )
                )
                Image(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(iconSize + bordersSize * 2)
                        .border(
                            width = bordersSize,
                            color = RadixTheme.colors.white.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .padding(bordersSize)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(White, White.copy(alpha = 0.73f))
                            ),
                            shape = RadixTheme.shapes.circle
                        )
                        .padding(4.dp),
                    painter = painterResource(id = R.drawable.ic_pool_units),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun CounterBox(
    modifier: Modifier,
    text: String,
    contentPadding: PaddingValues = PaddingValues()
) {
    Box(
        modifier = modifier
            .background(
                color = RadixTheme.colors.white.copy(alpha = 0.3f),
                shape = RadixTheme.shapes.roundedRectDefault
            ),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            modifier = Modifier
                .padding(contentPadding)
                .widthIn(min = 12.dp),
            text = text,
            style = RadixTheme.typography.body1Header.copy(fontSize = 11.sp, lineHeight = 18.sp),
            color = White,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
fun AssetsContentRowPreview() {
    RadixWalletTheme {
        Column(
            modifier = Modifier
                .padding(all = 32.dp)
                .background(
                    Brush.linearGradient(AccountGradientList[0]),
                    shape = RadixTheme.shapes.roundedRectMedium
                )
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountAssetsRow(assets = null, isLoading = true)

            val allFungibles = List(117) {
                Resource.FungibleResource(
                    resourceAddress = "resource_address",
                    ownedAmount = BigDecimal.valueOf(237659),
                    metadata = listOf(
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.NAME.key,
                            value = "Awesome token",
                            valueType = MetadataType.String
                        ),
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.SYMBOL.key,
                            value = "AWE",
                            valueType = MetadataType.String
                        ),
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.ICON_URL.key,
                            value = "https://c4.wallpaperflare.com/wallpaper/817/534/563/ave-bosque-fantasia-fenix-wallpaper-preview.jpg",
                            valueType = MetadataType.Url
                        )
                    )
                )
            }

            val nonFungibles = listOf(
                Resource.NonFungibleResource(
                    resourceAddress = "resource_address1",
                    amount = 1117,
                    items = List(1117) {
                        Resource.NonFungibleResource.Item(
                            collectionAddress = "resource_address1",
                            localId = Resource.NonFungibleResource.Item.ID.from("<f1_$it>")
                        )
                    },
                    metadata = listOf(
                        Metadata.Primitive(ExplicitMetadataKey.NAME.key, "F1", MetadataType.String)
                    )
                ),
                Resource.NonFungibleResource(
                    resourceAddress = "resource_address2",
                    amount = 3,
                    items = List(3) {
                        Resource.NonFungibleResource.Item(
                            collectionAddress = "resource_address2",
                            localId = Resource.NonFungibleResource.Item.ID.from("<nba_$it>")
                        )
                    },
                    metadata = listOf(
                        Metadata.Primitive(ExplicitMetadataKey.NAME.key, "NBA", MetadataType.String)
                    )
                )
            )

            AccountAssetsRow(
                assets = Assets(
                    fungibles = allFungibles,
                    nonFungibles = nonFungibles,
                    poolUnits = emptyList(),
                    validatorsWithStakes = emptyList()
                ),
                isLoading = false
            )
        }
    }
}
