package com.babylon.wallet.android.presentation.discover.blogposts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.discover.common.views.BlogPostItemView
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.clearFocusNestedScrollConnection
import com.babylon.wallet.android.utils.openInAppUrl
import com.radixdlt.sargon.BlogPost
import com.radixdlt.sargon.extensions.toUrl
import kotlinx.collections.immutable.persistentListOf

@Composable
fun BlogPostsScreen(
    modifier: Modifier = Modifier,
    viewModel: BlogPostsViewModel,
    onBackClick: () -> Unit
) {
    val state: BlogPostsViewModel.State by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toolbarColor = RadixTheme.colors.background.toArgb()

    BlogPostsContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onMessageShown = viewModel::onMessageShown,
        onBlogPostClick = {
            context.openInAppUrl(
                url = it.url.toString(),
                toolbarColor = toolbarColor
            )
        }
    )
}

@Composable
private fun BlogPostsContent(
    modifier: Modifier = Modifier,
    state: BlogPostsViewModel.State,
    onBackClick: () -> Unit,
    onMessageShown: () -> Unit,
    onBlogPostClick: (BlogPost) -> Unit
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
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.blogPosts_title),
                onBackClick = onBackClick,
                backIconType = BackIconType.Back,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        val listItems = remember(state) {
            if (state.isLoading) {
                List(5) {
                    null
                }
            } else {
                state.blogPosts
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(clearFocusNestedScrollConnection()),
            contentPadding = PaddingValues(
                RadixTheme.dimensions.paddingDefault
            ),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            items(listItems) { item ->
                BlogPostItemView(
                    item = item,
                    onClick = { item?.let { onBlogPostClick(it) } }
                )
            }
        }
    }
}

@Composable
@Preview
private fun BlogPostsPreview(
    @PreviewParameter(BlogPostsPreviewProvider::class) state: BlogPostsViewModel.State
) {
    RadixWalletPreviewTheme {
        BlogPostsContent(
            state = state,
            onBackClick = {},
            onMessageShown = {},
            onBlogPostClick = {}
        )
    }
}

class BlogPostsPreviewProvider : PreviewParameterProvider<BlogPostsViewModel.State> {

    override val values: Sequence<BlogPostsViewModel.State>
        get() = sequenceOf(
            BlogPostsViewModel.State(
                isLoading = false,
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
            BlogPostsViewModel.State(
                isLoading = true
            )
        )
}
