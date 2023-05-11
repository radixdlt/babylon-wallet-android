package com.babylon.wallet.android.presentation.wallet

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import java.math.BigDecimal

@Composable
fun AccountAssetsRow(
    modifier: Modifier = Modifier,
    resources: Resources?,
    iconSize: Dp = 30.dp,
    bordersSize: Dp = 1.dp,
    maxVisibleFungibles: Int = 5
) {
    if (resources == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(iconSize + bordersSize * 2)
                .clip(shape = RadixTheme.shapes.roundedRectMedium)
                .shimmer()
        )
    }

    AnimatedVisibility(
        modifier = modifier.wrapContentSize(),
        visible = resources != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        AssetsContent(
            modifier = modifier.fillMaxWidth(),
            resources = resources ?: Resources(emptyList(), emptyList()),
            iconSize = iconSize,
            maxVisibleFungibles = maxVisibleFungibles
        )
    }
}

@Composable
private fun AssetsContent(
    modifier: Modifier = Modifier,
    resources: Resources,
    iconSize: Dp,
    maxVisibleFungibles: Int,
    iconsOverlap: Dp = 10.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (sortedFungibles, remainingFungiblesCount) = remember(resources.fungibleResources) {
            val xrdResource = resources.fungibleResources.find { it.isXrd }
            val remainingResources = resources.fungibleResources.filterNot { it == xrdResource }

            val sorted = (listOf(xrdResource) + remainingResources).filterNotNull()
            sorted.take(maxVisibleFungibles) to (sorted.size - maxVisibleFungibles).coerceAtLeast(minimumValue = 0)
        }

        if (sortedFungibles.isNotEmpty() || resources.nonFungibleResources.isNotEmpty()) {
            sortedFungibles.forEachIndexed { index, fungible ->
                val iconModifier = Modifier
                    .zIndex(sortedFungibles.size - index.toFloat())
                    .offset(x = -(iconsOverlap * index))
                    .border(
                        width = 1.dp,
                        color = RadixTheme.colors.white.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .padding(1.dp)
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(RadixTheme.colors.defaultBackground.copy(alpha = 0.83f), shape = RadixTheme.shapes.circle)

                if (fungible.isXrd) {
                    Image(
                        modifier = iconModifier,
                        painter = painterResource(id = R.drawable.ic_xrd_token),
                        contentDescription = null
                    )
                } else {
                    AsyncImage(
                        modifier = iconModifier,
                        model = rememberImageUrl(fromUrl = fungible.iconUrl.toString(), size = ImageSize.SMALL),
                        placeholder = painterResource(id = R.drawable.ic_token),
                        error = painterResource(id = R.drawable.ic_token),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }

            val nonFungibleSectionOffset = iconsOverlap * (sortedFungibles.size - 1) + iconSize
            if (remainingFungiblesCount > 0) {
                CounterBox(
                    modifier = Modifier
                        .size(width = 56.dp, height = iconSize)
                        .offset(x = -nonFungibleSectionOffset),
                    text = "+$remainingFungiblesCount"
                )
            }

            if (resources.nonFungibleResources.isNotEmpty()) {
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))
                Box(
                    modifier = Modifier
                        .offset(x = -nonFungibleSectionOffset)
                ) {
                    CounterBox(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(width = 54.dp, height = 30.dp)
                            .align(Alignment.Center),
                        text = "${resources.nonFungibleResources.size}"
                    )

                    Image(
                        modifier = Modifier
                            .collectionImageModifier(32.dp)
                            .align(Alignment.CenterStart)
                            .padding(top = 4.dp), // Needed since the icon is not correctly centered.
                        painter = painterResource(id = R.drawable.ic_nfts),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

private fun Modifier.collectionImageModifier(size: Dp) = composed {
    size(size)
        .border(
            width = 1.dp,
            color = RadixTheme.colors.white.copy(alpha = 0.2f),
            shape = RoundedCornerShape(9.dp)
        )
        .padding(1.dp)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(White, White.copy(alpha = 0.73f))
            ),
            shape = RadixTheme.shapes.roundedRectSmall
        )
}

private fun Modifier.shimmer(
    colors: List<Color>? = null
): Modifier = composed {
    val shimmerColors = colors ?: listOf(
        RadixTheme.colors.defaultBackground.copy(alpha = 0.83f),
        RadixTheme.colors.defaultBackground.copy(alpha = 0.4f),
        Transparent,
        RadixTheme.colors.defaultBackground.copy(alpha = 0.4f),
        RadixTheme.colors.defaultBackground.copy(alpha = 0.83f)
    )
    var size by remember { mutableStateOf(IntSize.Zero) }

    val transition = rememberInfiniteTransition()

    val xOffset by transition.animateFloat(
        initialValue = -size.width.toFloat(),
        targetValue = size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000)
        )
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(xOffset, 0f),
            end = Offset(xOffset + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
private fun CounterBox(
    modifier: Modifier,
    text: String
) {
    Box(
        modifier = modifier
            .background(RadixTheme.colors.white.copy(alpha = 0.3f), shape = RadixTheme.shapes.roundedRectDefault),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            modifier = Modifier
                .padding(end = RadixTheme.dimensions.paddingSmall),
            text = text,
            style = RadixTheme.typography.body1Header.copy(fontSize = 11.sp, lineHeight = 18.sp),
            color = White
        )
    }
}


@Preview
@Composable
fun AssetsContentRowPreview() {
    RadixWalletTheme {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(AccountGradientList[0]),
                    shape = RadixTheme.shapes.roundedRectMedium
                )
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountAssetsRow(resources = null)

            AccountAssetsRow(
                resources = Resources(
                    fungibleResources = listOf(
                        AccountWithResources.Resource.FungibleResource(
                            resourceAddress = "resource_address",
                            amount = BigDecimal.valueOf(237659),
                            nameMetadataItem = NameMetadataItem("Radix"),
                            symbolMetadataItem = SymbolMetadataItem("XRD")
                        ),
                        AccountWithResources.Resource.FungibleResource(
                            resourceAddress = "resource_address",
                            amount = BigDecimal.valueOf(237659),
                            nameMetadataItem = NameMetadataItem("AWE"),
                            symbolMetadataItem = SymbolMetadataItem("AWE"),
                            iconUrlMetadataItem = IconUrlMetadataItem(url = Uri.parse("https://c4.wallpaperflare.com/wallpaper/817/534/563/ave-bosque-fantasia-fenix-wallpaper-preview.jpg"))
                        ),
                    ),
                    nonFungibleResources = listOf()
                )
            )
        }
    }
}
