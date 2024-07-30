@file:Suppress("CompositionLocalAllowlist")

package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.LinkConnectionStatusObserver.LinkConnectionsStatus
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun DevelopmentPreviewWrapper(
    modifier: Modifier = Modifier,
    linkConnectionsStatus: LinkConnectionsStatus? = null,
    devBannerState: DevBannerState = DevBannerState(false),
    content: @Composable () -> Unit
) {
    val devBannerHeight = remember { DevBannerHeight() }

    Box(modifier = modifier) {
        CompositionLocalProvider(LocalDevBannerState provides devBannerState) {
            CompositionLocalProvider(LocalDevBannerHeight provides devBannerHeight) {
                content()
            }
        }

        val density = LocalDensity.current
        if (devBannerState.isVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.orange2)
                    .statusBarsPadding()
                    .onGloballyPositioned { coordinates ->
                        devBannerHeight.height.value = with(density) { coordinates.size.height.toDp() }
                    }
                    .padding(RadixTheme.dimensions.paddingXSmall)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.common_developerDisclaimerText),
                    style = RadixTheme.typography.body2HighImportance,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                )

                if (linkConnectionsStatus != null) {
                    LazyRow(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                    ) {
                        items(items = linkConnectionsStatus.currentStatus()) { color ->
                            Canvas(
                                modifier = Modifier.size(12.dp),
                                onDraw = {
                                    drawCircle(color = color)
                                }
                            )
                        }
                    }
                }
            }
        } else if (linkConnectionsStatus != null) {
            LazyRow(
                modifier = Modifier
                    .statusBarsPadding()
                    .onGloballyPositioned { coordinates ->
                        devBannerHeight.height.value = with(density) { coordinates.size.height.toDp() }
                    }
                    .padding(RadixTheme.dimensions.paddingXSmall),
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                items(items = linkConnectionsStatus.currentStatus()) { color ->
                    Canvas(
                        modifier = Modifier.size(12.dp),
                        onDraw = {
                            drawCircle(color = color)
                        }
                    )
                }
            }
        } else {
            devBannerHeight.height.value = 0.dp
        }
    }
}

data class DevBannerState(
    val isVisible: Boolean = false,
    val connectionStatus: ImmutableList<Boolean> = persistentListOf()
)

data class DevBannerHeight(
    val height: MutableState<Dp> = mutableStateOf(0.dp),
)

val LocalDevBannerState = compositionLocalOf { DevBannerState() }
val LocalDevBannerHeight = compositionLocalOf { DevBannerHeight() }

val WindowInsets.Companion.statusBarsAndBanner: WindowInsets
    @Composable
    get() = WindowInsets.statusBars.add(WindowInsets(top = LocalDevBannerHeight.current.height.value))

@Preview(showBackground = true)
@Composable
fun DevelopmentBannerPreview() {
    RadixWalletTheme {
        DevelopmentPreviewWrapper(
            modifier = Modifier,
            linkConnectionsStatus = null,
            DevBannerState(
                isVisible = true,
                connectionStatus = persistentListOf()
            ),
            content = {}
        )
    }
}
