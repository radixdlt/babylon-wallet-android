package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onDismissRequest: () -> Unit
) {
    val windowInsets = WindowInsets(0)

    BoxWithConstraints(modifier = modifier) {
        val sheetHeight = maxHeight * heightFraction
        ModalBottomSheet(
            sheetState = sheetState,
            containerColor = RadixTheme.colors.defaultBackground,
            scrimColor = Color.Black.copy(alpha = 0.3f),
            dragHandle = {},
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
