package com.babylon.wallet.android.presentation.ui.composables.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme

private val DragHandleSize = DpSize(width = 38.dp, height = 4.dp)

@Composable
fun SheetHeader(
    modifier: Modifier = Modifier,
    title: String,
    onLeadingActionClicked: () -> Unit,
    leadingAction: @Composable () -> Unit = {
        IconButton(
            modifier = Modifier,
            onClick = onLeadingActionClicked
        ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                tint = RadixTheme.colors.gray1,
                contentDescription = "clear"
            )
        }
    }
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = RadixTheme.dimensions.paddingSmall),
    ) {
        DragHandler(modifier = Modifier.align(Alignment.TopCenter))

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = DragHandleSize.height + RadixTheme.dimensions.paddingSmall)
        ) {
            leadingAction()
        }

        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = DragHandleSize.height + RadixTheme.dimensions.paddingSmall),
            text = title,
            style = RadixTheme.typography.body1StandaloneLink,
            color = RadixTheme.colors.gray1
        )
    }
}

@Composable
private fun DragHandler(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = RadixTheme.dimensions.paddingSmall)
            .size(DragHandleSize)
            .background(color = RadixTheme.colors.gray4, shape = RadixTheme.shapes.circle)
    )
}
