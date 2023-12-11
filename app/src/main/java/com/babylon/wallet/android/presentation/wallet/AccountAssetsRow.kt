package com.babylon.wallet.android.presentation.wallet

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
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
    var visibleFungiblesCount by remember(maxVisibleFungibles) {
        mutableStateOf(maxVisibleFungibles)
    }
    ConstraintLayout(
        modifier = modifier
    ) {
        val (visibleFungibles, remainingFungiblesCount) = remember(assets.ownedFungibles, visibleFungiblesCount) {
            val all = assets.ownedFungibles
            all.take(visibleFungiblesCount) to (all.size - visibleFungiblesCount).coerceAtLeast(minimumValue = 0)
        }
        val nftsCount = remember(assets.nonFungibles) { assets.nonFungiblesSize() }
        val lsusCount = remember(assets.validatorsWithStakes) {
            assets.validatorsWithStakesSize()
        }
        val poolUnitCount = remember(assets.poolUnits) {
            assets.poolUnitsSize()
        }

        val fungibleRefs = visibleFungibles.map { createRef() }
        val fungibleCounterBoxRef = if (remainingFungiblesCount > 0) createRef() else null
        val nftsRowRef = if (nftsCount > 0) createRef() else null
        val lsusRowRef = if (lsusCount > 0) createRef() else null
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
            AssetTypeWithCounter(
                modifier = Modifier
                    .constrainAs(nftsRowRef) {
                        val lastRef = fungibleCounterBoxRef ?: fungibleRefs.lastOrNull()
                        start.linkTo(
                            anchor = lastRef?.end ?: parent.start,
                            margin = if (lastRef != null) 12.dp else 0.dp
                        )
                    }
                    .checkRenderedOutside {
                        if (visibleFungiblesCount > 1) {
                            visibleFungiblesCount -= 1
                        }
                    },
                icon = painterResource(id = R.drawable.ic_nfts),
                counter = nftsCount.toString(),
                iconSize = iconSize,
                bordersSize = bordersSize,
                shape = RoundedCornerShape(9.dp)
            )
        }

        if (lsusRowRef != null) {
            AssetTypeWithCounter(
                modifier = Modifier
                    .constrainAs(lsusRowRef) {
                        val lastRef = nftsRowRef ?: fungibleCounterBoxRef ?: fungibleRefs.lastOrNull()
                        start.linkTo(
                            anchor = lastRef?.end ?: parent.start,
                            margin = if (lastRef != null) 12.dp else 0.dp
                        )
                    }
                    .checkRenderedOutside {
                        if (visibleFungiblesCount > 1) {
                            visibleFungiblesCount -= 1
                        }
                    },
                icon = painterResource(id = R.drawable.ic_lsu),
                counter = lsusCount.toString(),
                iconSize = iconSize,
                bordersSize = bordersSize,
                shape = RadixTheme.shapes.circle
            )
        }

        if (poolUnitRowRef != null) {
            AssetTypeWithCounter(
                modifier = Modifier
                    .constrainAs(poolUnitRowRef) {
                        val lastRef = lsusRowRef ?: nftsRowRef ?: fungibleCounterBoxRef ?: fungibleRefs.lastOrNull()
                        start.linkTo(
                            anchor = lastRef?.end ?: parent.start,
                            margin = if (lastRef != null) 12.dp else 0.dp
                        )
                    }
                    .checkRenderedOutside {
                        if (visibleFungiblesCount > 1) {
                            visibleFungiblesCount -= 1
                        }
                    },
                icon = painterResource(id = R.drawable.ic_pool_units),
                counter = poolUnitCount.toString(),
                iconSize = iconSize,
                bordersSize = bordersSize,
                shape = RadixTheme.shapes.circle
            )
        }
    }
}

@Composable
private fun AssetTypeWithCounter(
    modifier: Modifier = Modifier,
    icon: Painter,
    counter: String,
    iconSize: Dp,
    bordersSize: Dp,
    shape: Shape
) {
    Box(
        modifier = modifier.height(iconSize)
    ) {
        CounterBox(
            modifier = Modifier.fillMaxHeight(),
            text = counter,
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
                    shape = shape
                )
                .padding(bordersSize)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(White, White.copy(alpha = 0.73f))
                    ),
                    shape = shape
                )
                .padding(4.dp),
            painter = icon,
            contentDescription = null
        )
    }
}

@Composable
private fun CounterBox(
    modifier: Modifier = Modifier,
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

@Composable
private fun Modifier.checkRenderedOutside(
    onRenderedOutside: (Float) -> Unit
): Modifier = this then Modifier.onGloballyPositioned {
    val parent = it.parentCoordinates?.size ?: return@onGloballyPositioned
    val maxX = it.positionInParent().x + it.size.width
    if (maxX > parent.width) {
        onRenderedOutside(maxX - parent.width)
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
                    poolUnits = List(120) {
                        PoolUnit(
                            stake = Resource.FungibleResource(
                                resourceAddress = "resource_address_$it",
                                ownedAmount = BigDecimal.valueOf(237659)
                            ),
                            pool = null
                        )
                    },
                    validatorsWithStakes = List(100) {
                        ValidatorWithStakes(
                            validatorDetail = ValidatorDetail(
                                address = "validator_$it",
                                totalXrdStake = BigDecimal.TEN
                            ),
                            liquidStakeUnit = LiquidStakeUnit(
                                Resource.FungibleResource(
                                    resourceAddress = "resource_address$it",
                                    ownedAmount = BigDecimal.valueOf(237659)
                                )
                            )
                        )
                    }
                ),
                isLoading = false
            )
        }
    }
}
