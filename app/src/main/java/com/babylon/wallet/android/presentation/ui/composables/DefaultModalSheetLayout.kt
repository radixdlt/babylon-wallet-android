package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.applyIf

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DefaultModalSheetLayout(
    modifier: Modifier,
    sheetState: ModalBottomSheetState,
    heightFraction: Float = 0.9f,
    enableImePadding: Boolean = false,
    sheetContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val sheetHeight = maxHeight * heightFraction
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetBackgroundColor = RadixTheme.colors.defaultBackground,
            scrimColor = Color.Black.copy(alpha = 0.3f),
            sheetShape = RadixTheme.shapes.roundedRectTopDefault,
            sheetContent = {
                Box(
                    modifier = Modifier.applyIf(enableImePadding, Modifier.imePadding())
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .clip(shape = RadixTheme.shapes.roundedRectTopMedium)
                ) {
                    sheetContent()
                }
            }
        ) {
            content()
        }
    }
}
