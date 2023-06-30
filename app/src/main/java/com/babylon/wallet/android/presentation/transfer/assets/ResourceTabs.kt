@file:OptIn(ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.transfer.assets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import kotlinx.coroutines.launch

@Composable
fun ResourcesTabs(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    selectedTab: ResourceTab,
    onTabSelected: (ResourceTab) -> Unit
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(selectedTab) {
        if (selectedTab.ordinal != pagerState.currentPage) {
            scope.launch { pagerState.animateScrollToPage(page = selectedTab.ordinal) }
        }
    }

    ResourcesTabs(
        modifier = modifier,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected
    )
}

@Composable
fun ResourcesTabs(
    modifier: Modifier = Modifier,
    selectedTab: ResourceTab,
    onTabSelected: (ResourceTab) -> Unit
) {
    val tabIndex = remember(selectedTab) {
        ResourceTab.values().indexOf(selectedTab)
    }
    TabRow(
        modifier = modifier.width(200.dp),
        selectedTabIndex = tabIndex,
        containerColor = Color.Transparent,
        divider = {},
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[tabIndex])
                    .fillMaxHeight()
                    .zIndex(-1f)
                    .background(RadixTheme.colors.gray1, RadixTheme.shapes.circle)
            )
        }
    ) {
        ResourceTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Tab(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onTabSelected(tab)
                    }
                },
                selectedContentColor = RadixTheme.colors.white,
                unselectedContentColor = RadixTheme.colors.gray1
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingMedium,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                    text = tab.name(),
                    style = RadixTheme.typography.body1HighImportance,
                )
            }
        }
    }
}

@Composable
private fun ResourceTab.name(): String = when (this) {
    ResourceTab.Tokens -> stringResource(id = R.string.account_tokens)
    ResourceTab.Nfts -> stringResource(id = R.string.account_nfts)
}

enum class ResourceTab {
    Tokens,
    Nfts
}
