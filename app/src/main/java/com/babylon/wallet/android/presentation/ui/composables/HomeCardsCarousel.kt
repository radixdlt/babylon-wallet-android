@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.ui.composables

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.presentation.wallet.cards.allowsDismiss
import com.babylon.wallet.android.presentation.wallet.cards.opensExternalLink
import com.radixdlt.sargon.HomeCard
import com.radixdlt.sargon.extensions.toUrl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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

        if (pagerState.pageCount > 1) {
            HorizontalPagerIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = RadixTheme.dimensions.paddingSmall),
                pagerState = pagerState,
                activeIndicatorWidth = 6.dp,
                inactiveIndicatorWidth = 4.dp,
                activeColor = RadixTheme.colors.iconSecondary,
                inactiveColor = RadixTheme.colors.backgroundTertiary
            )
        }
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
            containerColor = RadixTheme.colors.backgroundSecondary,
            contentColor = RadixTheme.colors.text,
            disabledContainerColor = RadixTheme.colors.backgroundSecondary,
            disabledContentColor = RadixTheme.colors.text
        )
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .throttleClickable(onClick = onClick)
        ) {
            val (titleView, descriptionView, endGraphicView, endIconView, closeIconView) = createRefs()
            createVerticalChain(titleView, descriptionView, chainStyle = ChainStyle.Packed)

            val endGraphicRes = card.EndGraphicRes()
            endGraphicRes?.let { painter ->
                Image(
                    modifier = Modifier.constrainAs(endGraphicView) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                        start.linkTo(parent.start)

                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                    painter = painter,
                    contentDescription = null,
                    alignment = Alignment.CenterEnd,
                    contentScale = ContentScale.FillHeight
                )
            }
            val titleWidthPercent = remember(endGraphicRes) {
                if (endGraphicRes == null) {
                    // More content fits as there is no overlap with the graphic
                    0.7f
                } else {
                    0.63f
                }
            }

            val titleStyle = RadixTheme.typography.body1Header
                .copy(color = RadixTheme.colors.text)
            Text(
                modifier = Modifier
                    .constrainAs(titleView) {
                        linkTo(
                            start = parent.start,
                            end = parent.end,
                            startMargin = 20.dp,
                            bias = 0f
                        )
                        width = Dimension.percent(titleWidthPercent)
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
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextBottom
                        )
                    ) {
                        Icon(
                            modifier = Modifier
                                .width(14.dp)
                                .height(12.dp),
                            painter = painterResource(id = R.drawable.ic_external_link),
                            contentDescription = null,
                            tint = RadixTheme.colors.iconSecondary
                        )
                    }
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                modifier = Modifier.constrainAs(descriptionView) {
                    linkTo(
                        start = titleView.start,
                        end = titleView.end
                    )
                    linkTo(
                        top = titleView.bottom,
                        bottom = parent.bottom,
                        bias = 0f
                    )
                    width = Dimension.fillToConstraints
                    height = Dimension.preferredWrapContent
                },
                text = card.description(),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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

            if (card.allowsDismiss()) {
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
                    tint = RadixTheme.colors.iconSecondary
                )
            }
        }
    }
}

@Composable
private fun HomeCard.title() = buildAnnotatedString {
    val title = when (this@title) {
        HomeCard.Connector -> stringResource(id = R.string.homePageCarousel_useDappsOnDesktop_title)
        HomeCard.ContinueRadQuest -> stringResource(id = R.string.homePageCarousel_rejoinRadquest_title)
        is HomeCard.Dapp -> stringResource(id = R.string.homePageCarousel_continueOnDapp_title)
        HomeCard.StartRadQuest -> stringResource(id = R.string.homePageCarousel_discoverRadix_title)
        HomeCard.DiscoverRadixDapps -> stringResource(id = R.string.homePageCarousel_discoverRadixDapps_title)
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
    is HomeCard.Dapp -> stringResource(id = R.string.homePageCarousel_continueOnDapp_text)
    HomeCard.StartRadQuest -> stringResource(id = R.string.homePageCarousel_discoverRadix_text)
    HomeCard.DiscoverRadixDapps -> stringResource(id = R.string.homePageCarousel_discoverRadixDapps_text)
}

@Composable
private fun HomeCard.EndIcon(
    modifier: Modifier = Modifier
) = when (this) {
    is HomeCard.Dapp -> {
        val uri = remember(iconUrl) {
            iconUrl?.let { Uri.parse(it.toString()) }
        }

        Thumbnail.DApp(
            modifier = modifier,
            dAppIconUrl = uri,
            dAppName = ""
        )
    }

    HomeCard.StartRadQuest,
    HomeCard.Connector,
    HomeCard.ContinueRadQuest,
    HomeCard.DiscoverRadixDapps -> {
    }
}

@Composable
private fun HomeCard.EndGraphicRes() = when (this) {
    HomeCard.Connector -> painterResource(id = R.drawable.ic_homecarousel_connect)
    HomeCard.ContinueRadQuest -> painterResource(id = R.drawable.ic_homecarousel_radquest)
    is HomeCard.Dapp -> null
    HomeCard.StartRadQuest -> painterResource(id = R.drawable.ic_homecarousel_radquest)
    HomeCard.DiscoverRadixDapps -> painterResource(id = R.drawable.ic_homecarousel_discover_dapps)
}

private const val INLINE_LINK_ICON = "link_icon"

@Preview
@Composable
fun HomeCardsCarouselContinueRadQuestPreviewLight() {
    RadixWalletPreviewTheme {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector,
                HomeCard.DiscoverRadixDapps
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
fun HomeCardsCarouselContinueRadQuestPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector,
                HomeCard.DiscoverRadixDapps
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
fun HomeCardsCarouselStartRadQuestPreviewLight() {
    RadixWalletPreviewTheme {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector,
                HomeCard.DiscoverRadixDapps
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
fun HomeCardsCarouselStartRadQuestPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector,
                HomeCard.DiscoverRadixDapps
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
fun HomeCardsCarouselDAppPreviewLight() {
    RadixWalletPreviewTheme {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector,
                HomeCard.DiscoverRadixDapps
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
fun HomeCardsCarouselDAppPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector,
                HomeCard.DiscoverRadixDapps
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
fun HomeCardsCarouselConnectorPreviewLight() {
    RadixWalletPreviewTheme {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector,
                HomeCard.DiscoverRadixDapps
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

@Preview
@Composable
fun HomeCardsCarouselConnectorPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector,
                HomeCard.DiscoverRadixDapps
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

@Preview(fontScale = 1.5f)
@Composable
fun HomeCardsCarouselScaledFontPreviewLight() {
    RadixWalletPreviewTheme {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector,
                HomeCard.DiscoverRadixDapps
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

@Preview(fontScale = 1.5f)
@Composable
fun HomeCardsCarouselScaledFontPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        val cards = remember {
            persistentListOf(
                HomeCard.ContinueRadQuest,
                HomeCard.StartRadQuest,
                HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                HomeCard.Connector,
                HomeCard.DiscoverRadixDapps
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
