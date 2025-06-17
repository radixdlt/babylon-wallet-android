package com.babylon.wallet.android.presentation.discover

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.discover.common.models.SocialLinkType
import com.babylon.wallet.android.presentation.discover.common.models.icon
import com.babylon.wallet.android.presentation.discover.common.models.title
import com.babylon.wallet.android.presentation.discover.common.views.BlogPostItemView
import com.babylon.wallet.android.presentation.discover.common.views.InfoGlossaryItemView
import com.babylon.wallet.android.presentation.discover.common.views.SimpleListItemView
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.HorizontalPagerIndicator
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.openInAppUrl
import com.babylon.wallet.android.utils.openUrl
import com.radixdlt.sargon.BlogPost
import com.radixdlt.sargon.extensions.toUrl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onInfoClick: (GlossaryItem) -> Unit,
    onMoreLearnClick: () -> Unit,
    onMoreBlogPostsClick: () -> Unit
) {
    val state: DiscoverViewModel.State by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toolbarColor = RadixTheme.colors.background.toArgb()

    DiscoverContent(
        state = state,
        onMessageShown = viewModel::onMessageShown,
        onInfoClick = onInfoClick,
        onMoreLearnClick = onMoreLearnClick,
        onSocialLinkClick = { context.openUrl(it.url) },
        onBlogPostClick = {
            context.openInAppUrl(
                url = it.url.toString(),
                toolbarColor = toolbarColor
            )
        },
        onMoreBlogPostsClick = onMoreBlogPostsClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverContent(
    modifier: Modifier = Modifier,
    state: DiscoverViewModel.State,
    onMessageShown: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onMoreLearnClick: () -> Unit,
    onSocialLinkClick: (SocialLinkType) -> Unit,
    onBlogPostClick: (BlogPost) -> Unit,
    onMoreBlogPostsClick: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = RadixTheme.colors.background)
            ) {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.discover_title),
                    onBackClick = {},
                    backIconType = BackIconType.None,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )

                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(vertical = RadixTheme.dimensions.paddingDefault)
        ) {
            SectionView(
                title = stringResource(R.string.discover_categoryBlogPosts_title),
                hasMore = true,
                onMoreClick = onMoreBlogPostsClick
            ) {
                val listItems = remember(state) {
                    if (state.isBlogPostsLoading) {
                        List(3) {
                            null
                        }.toPersistentList()
                    } else {
                        state.blogPosts
                    }
                }

                BlogPostsView(
                    items = listItems,
                    onClick = onBlogPostClick
                )
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            SectionView(
                title = stringResource(R.string.discover_categorySocials_title),
                hasMore = false
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                ) {
                    state.socialLinks.forEach { item ->
                        SimpleListItemView(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            title = item.title,
                            leadingIcon = {
                                Image(
                                    modifier = Modifier.size(44.dp),
                                    painter = painterResource(item.icon(isSystemInDarkTheme())),
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_external_link),
                                    contentDescription = null,
                                    tint = RadixTheme.colors.iconTertiary
                                )
                            },
                            onClick = { onSocialLinkClick(item) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            SectionView(
                title = stringResource(R.string.discover_categoryLearn_title),
                hasMore = true,
                onMoreClick = onMoreLearnClick
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                ) {
                    state.glossaryItems.forEach { item ->
                        InfoGlossaryItemView(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            item = item,
                            onClick = { onInfoClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionView(
    title: String,
    hasMore: Boolean,
    modifier: Modifier = Modifier,
    onMoreClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        CategoryHeader(
            title = title,
            hasMore = hasMore,
            onMoreClick = onMoreClick
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Column(
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            content()
        }
    }
}

@Composable
private fun CategoryHeader(
    title: String,
    hasMore: Boolean,
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.textSecondary,
            maxLines = 1
        )

        if (hasMore) {
            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

            RadixTextButton(
                text = stringResource(R.string.discover_seeMore_button),
                onClick = { onMoreClick?.invoke() },
                textStyle = RadixTheme.typography.body1Header,
                contentColor = RadixTheme.colors.textButton
            )
        }
    }
}

@Composable
private fun BlogPostsView(
    items: ImmutableList<BlogPost?>,
    onClick: (BlogPost) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        val pagerState = rememberPagerState(initialPage = 0) { items.size }
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(
                start = RadixTheme.dimensions.paddingDefault,
                end = RadixTheme.dimensions.paddingXXLarge
            ),
            pageSpacing = RadixTheme.dimensions.paddingDefault
        ) { page ->
            val item = items[page]

            BlogPostItemView(
                item = item,
                onClick = { item?.let { onClick(it) } }
            )
        }

        if (pagerState.pageCount > 1) {
            HorizontalPagerIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = RadixTheme.dimensions.paddingMedium),
                pagerState = pagerState,
                activeIndicatorWidth = 8.dp,
                inactiveIndicatorWidth = 6.dp,
                activeColor = RadixTheme.colors.iconSecondary,
                inactiveColor = RadixTheme.colors.iconTertiary
            )
        }
    }
}

@Composable
@Preview
private fun DiscoverPreview(
    @PreviewParameter(DiscoverPreviewProvider::class) state: DiscoverViewModel.State
) {
    RadixWalletPreviewTheme {
        DiscoverContent(
            state = state,
            onMessageShown = {},
            onInfoClick = {},
            onMoreLearnClick = {},
            onSocialLinkClick = {},
            onBlogPostClick = {},
            onMoreBlogPostsClick = {}
        )
    }
}

class DiscoverPreviewProvider : PreviewParameterProvider<DiscoverViewModel.State> {

    override val values: Sequence<DiscoverViewModel.State>
        get() = sequenceOf(
            DiscoverViewModel.State(
                isBlogPostsLoading = false,
                blogPosts = persistentListOf(
                    BlogPost(
                        name = "MVP Booster Grant Winners: RPFS, XRDegen, Liquify",
                        image = "https://google.com".toUrl(),
                        url = "https://google.com".toUrl()
                    ),
                    BlogPost(
                        name = "MVP Booster Grant Winners: RPFS, XRDegen, Liquify",
                        image = "https://google.com".toUrl(),
                        url = "https://google.com".toUrl()
                    )
                )
            ),
            DiscoverViewModel.State(
                isBlogPostsLoading = true
            )
        )
}
