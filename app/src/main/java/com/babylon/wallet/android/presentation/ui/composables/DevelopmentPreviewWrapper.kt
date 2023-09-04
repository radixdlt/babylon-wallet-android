package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun DevelopmentPreviewWrapper(
    modifier: Modifier = Modifier,
    devBannerState: DevBannerState = DevBannerState(false),
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier = modifier) {
        var bannerHeight by remember { mutableStateOf(0.dp) }
        CompositionLocalProvider(LocalDevBannerState provides devBannerState) {
            content(if (devBannerState.isVisible) PaddingValues(top = bannerHeight) else PaddingValues())
        }

        if (devBannerState.isVisible) {
            val density = LocalDensity.current
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.orange2)
                    .statusBarsPadding()
                    .onGloballyPositioned { coordinates ->
                        bannerHeight = with(density) { coordinates.size.height.toDp() }
                    }
                    .padding(RadixTheme.dimensions.paddingSmall),
                text = stringResource(R.string.common_developerDisclaimerText),
                style = RadixTheme.typography.body2HighImportance,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
        }
    }
}

data class DevBannerState(val isVisible: Boolean = false)

val LocalDevBannerState = compositionLocalOf { DevBannerState() }

@Preview(showBackground = true)
@Composable
fun DevelopmentBannerPreview() {
    RadixWalletTheme {
        DevelopmentPreviewWrapper(modifier = Modifier, DevBannerState(isVisible = true), content = {})
    }
}
