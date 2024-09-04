package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun LockScreenBackground(modifier: Modifier = Modifier, onTapToUnlock: (() -> Unit)? = null) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RadixTheme.colors.blue1)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(288.dp)
                .align(Alignment.Center),
            tint = Color.Unspecified
        )
        if (onTapToUnlock != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = RadixTheme.dimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Icon(
                    painter = painterResource(id = DSR.ic_lock),
                    contentDescription = null,
                    tint = RadixTheme.colors.blue1,
                    modifier = Modifier
                        .clip(RadixTheme.shapes.circle)
                        .clickable {
                            onTapToUnlock()
                        }
                        .size(40.dp)
                        .background(RadixTheme.colors.white, shape = RadixTheme.shapes.circle)
                        .padding(RadixTheme.dimensions.paddingSmall)
                )
                Text(
                    modifier = Modifier
                        .clickable { onTapToUnlock() },
                    text = stringResource(R.string.splash_tapAnywhereToUnlock),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.white,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview
@Composable
private fun LockScreenBackgroundPreview() {
    RadixWalletPreviewTheme {
        LockScreenBackground()
    }
}
