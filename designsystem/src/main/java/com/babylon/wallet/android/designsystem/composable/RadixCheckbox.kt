package com.babylon.wallet.android.designsystem.composable

import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.babylon.wallet.android.designsystem.theme.RadixTheme

object RadixCheckboxDefaults {
    @Composable
    fun colors() = CheckboxDefaults.colors().copy(
        checkedCheckmarkColor = RadixTheme.colors.background,
        uncheckedCheckmarkColor = Color.Transparent,
        checkedBoxColor = RadixTheme.colors.icon,
        uncheckedBoxColor = Color.Transparent,
        disabledCheckedBoxColor = RadixTheme.colors.icon.copy(alpha = 0.2f),
        disabledUncheckedBoxColor = Color.Transparent,
        disabledIndeterminateBoxColor = Color.Transparent,
        checkedBorderColor = RadixTheme.colors.icon,
        uncheckedBorderColor = RadixTheme.colors.icon.copy(alpha = 0.5f),
        disabledBorderColor = RadixTheme.colors.icon.copy(alpha = 0.2f),
        disabledUncheckedBorderColor = RadixTheme.colors.icon.copy(alpha = 0.2f),
        disabledIndeterminateBorderColor = RadixTheme.colors.icon.copy(alpha = 0.2f),
    )    
}