package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.utils.isKeyboardVisible

fun Modifier.dynamicImePadding(
    padding: PaddingValues,
    keyboardVisibleBottomPadding: Dp? = null
): Modifier {
    return this.composed {
        padding(
            top = padding.calculateTopPadding(),
            bottom = if (isKeyboardVisible()) {
                keyboardVisibleBottomPadding ?: RadixTheme.dimensions.paddingSmall
            } else {
                padding.calculateBottomPadding()
            }
        )
    }
}
