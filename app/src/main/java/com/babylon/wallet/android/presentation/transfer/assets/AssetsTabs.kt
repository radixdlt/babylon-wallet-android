package com.babylon.wallet.android.presentation.transfer.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun AssetsTabs(
    modifier: Modifier = Modifier,
    selectedTab: ResourceTab,
    onTabSelected: (ResourceTab) -> Unit
) {
    val tabIndex = remember(selectedTab) {
        ResourceTab.values().indexOf(selectedTab)
    }
    TabRow(
        modifier = modifier,
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
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Tab(
                modifier = Modifier.wrapContentWidth(),
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
    ResourceTab.PoolUnits -> stringResource(id = R.string.account_poolUnits)
}

enum class ResourceTab {
    Tokens,
    Nfts,
    PoolUnits
}
