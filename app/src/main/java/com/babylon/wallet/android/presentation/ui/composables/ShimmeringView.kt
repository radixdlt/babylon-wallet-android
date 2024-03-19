package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder

@Composable
fun ShimmeringView(
    modifier: Modifier = Modifier,
    isVisible: Boolean = false
) {
    Box(
        modifier = modifier
            .radixPlaceholder(
                visible = isVisible,
                shape = RadixTheme.shapes.roundedRectXSmall,
            )
    )
}
