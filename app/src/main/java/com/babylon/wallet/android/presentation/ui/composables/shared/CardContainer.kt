package com.babylon.wallet.android.presentation.ui.composables.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow

@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    castsShadow: Boolean = true,
    isOutlined: Boolean = false,
    containerColor: Color = RadixTheme.colors.card,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .applyIf(
                castsShadow,
                Modifier
                    .defaultCardShadow(elevation = 6.dp)
                    .clip(RadixTheme.shapes.roundedRectMedium)
            )
            .applyIf(
                isOutlined,
                Modifier.border(
                    width = 1.dp,
                    color = RadixTheme.colors.border,
                    shape = RadixTheme.shapes.roundedRectDefault
                )
            )
            .fillMaxWidth()
            .background(
                color = containerColor,
                shape = RadixTheme.shapes.roundedRectDefault
            )
    ) {
        content()
    }
}
