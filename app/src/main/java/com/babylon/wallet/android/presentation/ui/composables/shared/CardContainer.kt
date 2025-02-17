package com.babylon.wallet.android.presentation.ui.composables.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow

@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .defaultCardShadow(elevation = 6.dp)
            .clip(RadixTheme.shapes.roundedRectMedium)
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.white,
                shape = RadixTheme.shapes.roundedRectDefault
            )
    ) {
        content()
    }
}
