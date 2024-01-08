package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.applyIf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultModalSheetLayout(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    heightFraction: Float = 0.9f,
    enableImePadding: Boolean = false,
    wrapContent: Boolean = false,
    sheetContent: @Composable () -> Unit,
    showDragHandle: Boolean = false,
    containerColor: Color = RadixTheme.colors.defaultBackground,
    onDismissRequest: () -> Unit
) {
    val windowInsets = WindowInsets(0)

    BoxWithConstraints(modifier = modifier) {
        val sheetHeight = maxHeight * heightFraction
        ModalBottomSheet(
            sheetState = sheetState,
            containerColor = containerColor,
            scrimColor = Color.Black.copy(alpha = 0.3f),
            dragHandle = {
                if (showDragHandle) {
                    DefaultModalSheetDragHandle()
                }
            },
            windowInsets = windowInsets,
            shape = RadixTheme.shapes.roundedRectTopDefault,
            content = {
                Box(
                    modifier = Modifier
                        .applyIf(enableImePadding, Modifier.imePadding())
                        .fillMaxWidth()
                        .applyIf(wrapContent, Modifier.wrapContentHeight())
                        .applyIf(!wrapContent, Modifier.height(sheetHeight))
                        .clip(shape = RadixTheme.shapes.roundedRectTopMedium)
                ) {
                    sheetContent()
                }
            },
            onDismissRequest = onDismissRequest
        )
    }
}

@Composable
private fun DefaultModalSheetDragHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(top = RadixTheme.dimensions.paddingSmall)
            .size(38.dp, 4.dp)
            .background(color = RadixTheme.colors.gray4, shape = RadixTheme.shapes.circle)
    )
}
