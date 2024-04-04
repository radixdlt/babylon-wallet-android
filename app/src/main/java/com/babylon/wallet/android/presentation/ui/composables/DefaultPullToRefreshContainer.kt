package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DefaultPullToRefreshContainer(
    modifier: Modifier = Modifier,
    canRefresh: Boolean = true,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    val pullToRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh,
        refreshingOffset = 116.dp
    )
    Box(
        modifier = modifier
            .pullRefresh(pullToRefreshState, enabled = canRefresh)
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        content()
        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = isRefreshing,
            state = pullToRefreshState,
            contentColor = RadixTheme.colors.gray1,
            backgroundColor = RadixTheme.colors.defaultBackground,
        )
    }
}
