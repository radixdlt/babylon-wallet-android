@file:Suppress("TooManyFunctions")
@file:OptIn(ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.ui.composables

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Url
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.sargon.toUrl
import kotlin.math.absoluteValue
import kotlin.math.sign

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeCardsCarousel(
    modifier: Modifier = Modifier,
    cards: ImmutableList<HomeCard>,
    initialPage: Int = 0,
    onClick: (HomeCard) -> Unit,
    onCloseClick: (HomeCard) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        val pagerState = rememberPagerState(initialPage = initialPage) { cards.size }
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingLarge),
            pageSpacing = RadixTheme.dimensions.paddingMedium
        ) { page ->
            val card = cards[page]
            CardView(
                card = card,
                onClick = {
                    onClick(card)
                },
                onCloseClick = {
                    onCloseClick(card)
                }
            )
        }

        HorizontalPagerIndicator(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = RadixTheme.dimensions.paddingSmall),
            pagerState = pagerState,
            activeIndicatorWidth = 6.dp,
            inactiveIndicatorWidth = 4.dp,
            activeColor = RadixTheme.colors.gray2,
            inactiveColor = RadixTheme.colors.gray4
        )
    }
}

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
private fun CardView(
    modifier: Modifier = Modifier,
    card: HomeCard,
    onClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(105.dp)
            .fillMaxWidth(),
        shape = RadixTheme.shapes.roundedRectMedium,
        colors = CardColors(
            containerColor = RadixTheme.colors.gray5,
            contentColor = RadixTheme.colors.gray1,
            disabledContainerColor = RadixTheme.colors.gray5,
            disabledContentColor = RadixTheme.colors.gray1
        )
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxSize().throttleClickable(onClick = onClick)) {
            val (titleView, descriptionView, endGraphicView, endIconView, closeIconView) = createRefs()
            createVerticalChain(titleView, descriptionView, chainStyle = ChainStyle.Packed)

            card.EndGraphic(
                modifier = Modifier.constrainAs(endGraphicView) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)

                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
            )

            val titleStyle = RadixTheme.typography.body1Header
            Text(
                modifier = Modifier
                    .constrainAs(titleView) {
                        linkTo(
                            start = parent.start,
                            end = parent.end,
                            startMargin = 20.dp,
                            endMargin = 20.dp,
                            bias = 0f
                        )
                        width = Dimension.percent(0.63f)
                        height = Dimension.wrapContent
                    }
                    .padding(bottom = RadixTheme.dimensions.paddingSmall),
                text = card.title(),
                style = titleStyle,
                inlineContent = mapOf(
                    INLINE_LINK_ICON to InlineTextContent(
                        placeholder = Placeholder(
                            width = titleStyle.fontSize,
                            height = titleStyle.fontSize,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_external_link),
                            contentDescription = null,
                            tint = RadixTheme.colors.gray2
                        )
                    }
                )
            )

            Text(
                modifier = Modifier.constrainAs(descriptionView) {
                    linkTo(start = titleView.start, end = titleView.end)

                    width = Dimension.fillToConstraints
                    height = Dimension.wrapContent
                },
                text = card.description(),
                style = RadixTheme.typography.body2Regular,
            )

            card.EndIcon(
                modifier = Modifier.constrainAs(endIconView) {
                    end.linkTo(parent.end, margin = 20.dp)

                    width = Dimension.value(56.dp)
                    height = Dimension.value(56.dp)

                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
            )

            Icon(
                modifier = Modifier
                    .throttleClickable(onClick = onCloseClick)
                    .padding(RadixTheme.dimensions.paddingSmall)
                    .size(16.dp)
                    .constrainAs(closeIconView) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    },
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                contentDescription = null,
                tint = RadixTheme.colors.gray2
            )
        }
    }
}

@Composable
private fun HomeCard.title() = buildAnnotatedString {
    val title = when (this@title) {
        HomeCard.Connector -> stringResource(id = R.string.homePageCarousel_useDappsOnDesktop_title)
        HomeCard.ContinueRadQuest -> stringResource(id = R.string.homePageCarousel_rejoinRadquest_title)
        is HomeCard.DApp -> stringResource(id = R.string.homePageCarousel_continueOnDapp_title)
        HomeCard.StartRadQuest -> stringResource(id = R.string.homePageCarousel_discoverRadix_title)
    }
    append(title)

    if (opensExternalLink()) {
        append("  ")
        appendInlineContent(id = INLINE_LINK_ICON)
    }
}

@Composable
private fun HomeCard.description() = when (this) {
    HomeCard.Connector -> stringResource(id = R.string.homePageCarousel_useDappsOnDesktop_text)
    HomeCard.ContinueRadQuest -> stringResource(id = R.string.homePageCarousel_rejoinRadquest_text)
    is HomeCard.DApp -> stringResource(id = R.string.homePageCarousel_continueOnDapp_text)
    HomeCard.StartRadQuest -> stringResource(id = R.string.homePageCarousel_discoverRadix_text)
}

@Composable
private fun HomeCard.EndIcon(
    modifier: Modifier = Modifier
) = when (this) {
    HomeCard.Connector -> {}
    HomeCard.ContinueRadQuest -> {}
    is HomeCard.DApp -> {
        val uri = remember(iconUrl) {
            iconUrl?.let { Uri.parse(it.toString()) }
        }

        Thumbnail.DApp(
            modifier = modifier,
            dAppIconUrl = uri,
            dAppName = ""
        )
    }

    HomeCard.StartRadQuest -> {}
}

@Composable
private fun HomeCard.EndGraphic(
    modifier: Modifier = Modifier
) = when (this) {
    HomeCard.Connector -> painterResource(id = R.drawable.ic_homecarousel_connect)
    HomeCard.ContinueRadQuest -> painterResource(id = R.drawable.ic_radquest_bg)
    is HomeCard.DApp -> null
    HomeCard.StartRadQuest -> painterResource(id = R.drawable.ic_radquest_bg)
}.let { painter ->
    if (painter != null) {
        Image(
            modifier = modifier,
            painter = painter,
            contentDescription = null,
            alignment = Alignment.CenterEnd,
            contentScale = ContentScale.FillHeight
        )
    }
}

private const val INLINE_LINK_ICON = "link_icon"

@Composable
private fun HorizontalPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    pageIndexMapping: (Int) -> Int = { it },
    activeColor: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    inactiveColor: Color = activeColor.copy(ContentAlpha.disabled),
    activeIndicatorWidth: Dp = 8.dp,
    activeIndicatorHeight: Dp = activeIndicatorWidth,
    inactiveIndicatorWidth: Dp = 8.dp,
    inactiveIndicatorHeight: Dp = inactiveIndicatorWidth,
    spacing: Dp = activeIndicatorWidth,
    indicatorShape: Shape = CircleShape,
) {
    val activeIndicatorWidthPx = LocalDensity.current.run { activeIndicatorWidth.roundToPx() }
    val spacingPx = LocalDensity.current.run { spacing.roundToPx() }

    val inactiveWidth = inactiveIndicatorWidth.coerceAtMost(activeIndicatorWidth)
    val inactiveHeight = inactiveIndicatorHeight.coerceAtMost(activeIndicatorHeight)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val indicatorModifier = Modifier
                .size(
                    width = activeIndicatorWidth,
                    height = activeIndicatorHeight
                )
                .padding(
                    horizontal = (activeIndicatorWidth - inactiveWidth) / 2,
                    vertical = (activeIndicatorHeight - inactiveHeight) / 2
                )
                .background(color = inactiveColor, shape = indicatorShape)

            repeat(pagerState.pageCount) {
                Box(indicatorModifier)
            }
        }

        Box(
            Modifier
                .offset {
                    val position = pageIndexMapping(pagerState.currentPage)
                    val offset = pagerState.currentPageOffsetFraction
                    val next = pageIndexMapping(pagerState.currentPage + offset.sign.toInt())
                    val scrollPosition = ((next - position) * offset.absoluteValue + position)
                        .coerceIn(
                            0f,
                            (pagerState.pageCount - 1)
                                .coerceAtLeast(0)
                                .toFloat()
                        )

                    IntOffset(
                        x = ((spacingPx + activeIndicatorWidthPx) * scrollPosition).toInt(),
                        y = 0
                    )
                }
                .size(width = activeIndicatorWidth, height = activeIndicatorHeight)
                .then(
                    if (pagerState.pageCount > 0) {
                        Modifier.background(
                            color = activeColor,
                            shape = indicatorShape,
                        )
                    } else {
                        Modifier
                    }
                )
        )
    }
}

// To be replaced with sargon
sealed interface HomeCard {
    data object StartRadQuest : HomeCard
    data object ContinueRadQuest : HomeCard
    data class DApp(val iconUrl: Url?) : HomeCard
    data object Connector : HomeCard
}

private fun HomeCard.opensExternalLink() = this is HomeCard.StartRadQuest

@Preview
@Composable
fun HomeCardsCarouselContinueRadQuestPreview() {
    RadixWalletPreviewTheme {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.DApp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector
            )
        }
        HomeCardsCarousel(
            modifier = Modifier.fillMaxWidth(),
            cards = cards,
            onClick = {},
            onCloseClick = {}
        )
    }
}

@Preview
@Composable
fun HomeCardsCarouselStartRadQuestPreview() {
    RadixWalletPreviewTheme {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.DApp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector
            )
        }
        HomeCardsCarousel(
            modifier = Modifier.fillMaxWidth(),
            cards = cards,
            initialPage = 1,
            onClick = {},
            onCloseClick = {}
        )
    }
}

@Preview
@Composable
fun HomeCardsCarouselDAppPreview() {
    RadixWalletPreviewTheme {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.DApp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector
            )
        }
        HomeCardsCarousel(
            modifier = Modifier.fillMaxWidth(),
            cards = cards,
            initialPage = 2,
            onClick = {},
            onCloseClick = {}
        )
    }
}

@Preview
@Composable
fun HomeCardsCarouselConnectorPreview() {
    RadixWalletPreviewTheme {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.DApp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector
            )
        }
        HomeCardsCarousel(
            modifier = Modifier.fillMaxWidth(),
            cards = cards,
            initialPage = 3,
            onClick = {},
            onCloseClick = {}
        )
    }
}