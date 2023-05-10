package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.AccountWithResources

private const val MAX_ASSETS_DISPLAYED = 10
private const val RELATIVE_PADDING = 0.7f

@Suppress("UnstableCollections")
@Composable
fun AssetIconRowView(
    assets: List<AccountWithResources.FungibleResource>,
    modifier: Modifier = Modifier,
    circleSize: Int = 30,
    fontSize: Int = 10,
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        var paddingStart = 0f
        val overflowingAssetsCount = assets.size - MAX_ASSETS_DISPLAYED
        for (i in assets.indices) {
            if (i >= MAX_ASSETS_DISPLAYED) {
                Box(
                    modifier = Modifier
                        .zIndex(assets.size - i.toFloat())
                        .offset(paddingStart.dp - (circleSize * RELATIVE_PADDING).dp, 0.dp)
                        .background(RadixTheme.colors.white.copy(alpha = 0.3f), shape = RadixTheme.shapes.circle)
                        .defaultMinSize((circleSize * 1.8).dp, circleSize.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.padding(start = (circleSize * 0.8).dp),
                        text = "+$overflowingAssetsCount",
                        style = RadixTheme.typography.body2Regular.copy(fontSize = fontSize.sp),
                        color = RadixTheme.colors.gray1
                    )
                }
                break
            } else {
                Box(
                    modifier = Modifier
                        .zIndex(assets.size - i.toFloat())
                        .offset(paddingStart.dp, 0.dp)
                        .background(RadixTheme.colors.gray3, shape = RadixTheme.shapes.circle)
                        .defaultMinSize(circleSize.dp, circleSize.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = assets[i].symbol,
                        style = RadixTheme.typography.body2Regular.copy(fontSize = fontSize.sp),
                        color = RadixTheme.colors.gray1
                    )
                }
            }
            paddingStart += circleSize * RELATIVE_PADDING
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AssetIconRowPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            AssetIconRowView(
                assets = sampleFungibleResources()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AssetIconRowOverflowPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            AssetIconRowView(
                assets = sampleFungibleResources() +
                    sampleFungibleResources() + sampleFungibleResources() + sampleFungibleResources()
            )
        }
    }
}
