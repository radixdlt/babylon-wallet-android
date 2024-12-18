package com.babylon.wallet.android.presentation.settings.securitycenter.common.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun FactorsContainerView(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.gray5,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingMedium,
                vertical = RadixTheme.dimensions.paddingDefault
            )
    ) {
        content()
    }
}