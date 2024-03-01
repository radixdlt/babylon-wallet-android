package com.babylon.wallet.android.presentation.history.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.bubbleShape
import com.babylon.wallet.android.presentation.ui.composables.ExpandableText

@Composable
fun HistoryMessageContent(text: String, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val bubbleShape = remember {
        bubbleShape(
            density = density,
            cornerRadius = 12.dp,
            onlyTopCorners = true
        )
    }
    ExpandableText(
        modifier = modifier
            .fillMaxWidth()
            .padding(1.dp)
            .background(RadixTheme.colors.gray4, shape = bubbleShape)
            .padding(RadixTheme.dimensions.paddingMedium),
        text = text,
        collapsedLines = 2,
        style = RadixTheme.typography.body2Regular.copy(color = RadixTheme.colors.gray1),
        toggleStyle = RadixTheme.typography.body2Header.copy(color = RadixTheme.colors.blue1, fontSize = 14.sp)
    )
}
