package com.babylon.wallet.android.presentation.account.settings.specificassets.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun SpecificAssetsTabs(
    modifier: Modifier = Modifier,
    selectedTab: SpecificAssetsTab,
    onTabSelected: (SpecificAssetsTab) -> Unit
) {
    val tabIndex = remember(selectedTab) {
        SpecificAssetsTab.entries.indexOf(selectedTab)
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
                    .padding(2.dp)
                    .background(RadixTheme.colors.white, RadixTheme.shapes.roundedRectSmall)
            )
        }
    ) {
        SpecificAssetsTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Tab(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onTabSelected(tab)
                    }
                },
                selectedContentColor = RadixTheme.colors.gray1,
                unselectedContentColor = RadixTheme.colors.gray1
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingMedium,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                    text = tab.name(),
                    style = if (isSelected) RadixTheme.typography.body1Header else RadixTheme.typography.body1Regular,
                )
            }
        }
    }
}

@Composable
private fun SpecificAssetsTab.name(): String = when (this) {
    SpecificAssetsTab.Allowed -> stringResource(id = R.string.accountSettings_specificAssetsDeposits_allow)
    SpecificAssetsTab.Denied -> stringResource(id = R.string.accountSettings_specificAssetsDeposits_deny)
}

enum class SpecificAssetsTab {
    Allowed,
    Denied
}

@Composable
@Preview
private fun SpecificAssetsTabsPreview() {
    RadixWalletPreviewTheme {
        SpecificAssetsTabs(
            modifier = Modifier.fillMaxWidth(),
            selectedTab = SpecificAssetsTab.Allowed,
            onTabSelected = {}
        )
    }
}
