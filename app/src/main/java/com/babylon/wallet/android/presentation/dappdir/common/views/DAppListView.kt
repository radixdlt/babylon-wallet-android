package com.babylon.wallet.android.presentation.dappdir.common.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.Category
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.DAppWithDetails
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListState
import com.babylon.wallet.android.presentation.dappdir.common.models.title
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.discover.common.views.LoadingErrorView
import com.babylon.wallet.android.presentation.ui.composables.utils.clearFocusNestedScrollConnection
import com.radixdlt.sargon.AccountAddress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DAppListView(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    state: DAppListState,
    emptyStateTitle: String,
    onRefresh: () -> Unit,
    onDAppClick: (AccountAddress) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    Box(
        modifier = modifier
    ) {
        val directory = remember(state) {
            if (state.isLoading) {
                List(10) {
                    null
                }
            } else {
                state.items
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(clearFocusNestedScrollConnection())
                .pullToRefresh(
                    state = pullToRefreshState,
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh
                ),
            contentPadding = padding.plus(
                PaddingValues(RadixTheme.dimensions.paddingDefault)
            ),
            userScrollEnabled = !state.isLoading,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            items(
                items = directory,
            ) { item ->
                when (item) {
                    is Category -> Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = item.type.title(),
                        style = RadixTheme.typography.header,
                        color = RadixTheme.colors.text,
                        maxLines = 1
                    )

                    is DAppWithDetails, null -> DAppCard(
                        modifier = Modifier.fillMaxWidth(),
                        details = item,
                        onClick = {
                            if (item != null) {
                                onDAppClick(item.dAppDefinitionAddress)
                            }
                        }
                    )
                }
            }
        }

        when {
            state.errorLoading -> LoadingErrorView(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .align(Alignment.Center),
                title = stringResource(R.string.dappDirectory_error_heading),
                subtitle = stringResource(R.string.dappDirectory_error_message)
            )

            state.isEmpty -> DAppsEmptyStateView(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .align(Alignment.Center),
                title = emptyStateTitle,
                onInfoClick = onInfoClick
            )
        }

        PullToRefreshDefaults.Indicator(
            modifier = Modifier
                .padding(padding)
                .align(Alignment.TopCenter),
            state = pullToRefreshState,
            isRefreshing = state.isRefreshing,
            color = RadixTheme.colors.icon,
            containerColor = RadixTheme.colors.backgroundTertiary
        )
    }
}
