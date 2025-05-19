package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions

@Composable
fun GrayBackgroundWrapper(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = dimensions.paddingDefault),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .background(RadixTheme.colors.backgroundSecondary)
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}
