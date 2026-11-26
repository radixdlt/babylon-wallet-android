package com.babylon.wallet.android.presentation.transfer.assets

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import kotlin.math.roundToInt

@Composable
fun AssetsTabs(
    modifier: Modifier = Modifier,
    selectedTab: AssetsTab,
    onTabSelected: (AssetsTab) -> Unit
) {
    val tabBounds = remember { mutableStateMapOf<AssetsTab, TabBounds>() }
    val selectedBounds = tabBounds[selectedTab]
    val indicatorX by animateIntAsState(
        targetValue = selectedBounds?.position?.x?.roundToInt() ?: 0,
        label = "assetTabIndicatorX"
    )
    val indicatorY by animateIntAsState(
        targetValue = selectedBounds?.position?.y?.roundToInt() ?: 0,
        label = "assetTabIndicatorY"
    )
    val indicatorWidth by animateIntAsState(
        targetValue = selectedBounds?.width ?: 0,
        label = "assetTabIndicatorWidth"
    )
    val indicatorHeight by animateIntAsState(
        targetValue = selectedBounds?.height ?: 0,
        label = "assetTabIndicatorHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        if (selectedBounds != null) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorX, indicatorY) }
                    .width(with(androidx.compose.ui.platform.LocalDensity.current) { indicatorWidth.toDp() })
                    .height(with(androidx.compose.ui.platform.LocalDensity.current) { indicatorHeight.toDp() })
                    .background(
                        color = RadixTheme.colors.chipBackground,
                        shape = RadixTheme.shapes.circle
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetsTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            tabBounds[tab] = TabBounds(
                                position = coordinates.positionInParent(),
                                width = coordinates.size.width,
                                height = coordinates.size.height
                            )
                        }
                        .clickable(
                            enabled = !isSelected,
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            onTabSelected(tab)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingSmall
                        ),
                        text = tab.name(),
                        style = RadixTheme.typography.body1HighImportance,
                        color = if (isSelected) White else RadixTheme.colors.text
                    )
                }
            }
        }
    }
}

private data class TabBounds(
    val position: Offset,
    val width: Int,
    val height: Int
)

@Composable
private fun AssetsTab.name(): String = when (this) {
    AssetsTab.Tokens -> stringResource(id = R.string.account_tokens)
    AssetsTab.Nfts -> stringResource(id = R.string.account_nfts)
    AssetsTab.Staking -> stringResource(id = R.string.account_staking)
    AssetsTab.PoolUnits -> stringResource(id = R.string.account_poolUnits)
}

enum class AssetsTab {
    Tokens,
    Nfts,
    Staking,
    PoolUnits;

    fun toInfoTag(): GlossaryItem = when (this) {
        Tokens -> GlossaryItem.tokens
        Nfts -> GlossaryItem.nfts
        Staking -> GlossaryItem.networkstaking
        PoolUnits -> GlossaryItem.poolunits
    }
}
