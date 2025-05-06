package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixThemeConfig
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

// TODO Theme
@Composable
fun RadixSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean = true,
) {
    Switch(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        // Specifying any thumbContent makes the handle size consistent between the states
        thumbContent = {},
        colors = RadixSwitchDefaults.colors(),
    )
}

@Preview
@Composable
private fun RadixSwitchPreviewLight() {
    RadixWalletTheme {
        Row(
            modifier = Modifier.background(RadixTheme.colors.background),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RadixSwitch(checked = false, onCheckedChange = {})

            RadixSwitch(checked = true, onCheckedChange = {})

            RadixSwitch(checked = false, enabled = false, onCheckedChange = {})

            RadixSwitch(checked = true, enabled = false, onCheckedChange = {})
        }
    }
}

@Preview
@Composable
private fun RadixSwitchPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isSystemDarkTheme = true)) {
        Row(
            modifier = Modifier.background(RadixTheme.colors.background),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RadixSwitch(checked = false, onCheckedChange = {})

            RadixSwitch(checked = true, onCheckedChange = {})

            RadixSwitch(checked = false, enabled = false, onCheckedChange = {})

            RadixSwitch(checked = true, enabled = false, onCheckedChange = {})
        }
    }
}

object RadixSwitchDefaults {

    @Composable
    fun colors() = SwitchDefaults.colors(
        checkedThumbColor = RadixTheme.colors.background,
        checkedTrackColor = RadixTheme.colors.icon,
        checkedBorderColor = RadixTheme.colors.icon,
        checkedIconColor = Color.Transparent,
        uncheckedThumbColor = RadixTheme.colors.background,
        uncheckedTrackColor = RadixTheme.colors.iconTertiary,
        uncheckedBorderColor = RadixTheme.colors.iconTertiary,
        uncheckedIconColor = Color.Transparent,
        disabledCheckedThumbColor = RadixTheme.colors.background.copy(alpha = 0.4f),
        disabledCheckedTrackColor = RadixTheme.colors.iconTertiary.copy(alpha = 0.6f),
        disabledCheckedBorderColor = Color.Transparent,
        disabledCheckedIconColor = Color.Transparent,
        disabledUncheckedThumbColor = RadixTheme.colors.background.copy(alpha = 0.4f),
        disabledUncheckedTrackColor = RadixTheme.colors.iconTertiary.copy(alpha = 0.6f),
        disabledUncheckedBorderColor = Color.Transparent,
        disabledUncheckedIconColor = Color.Transparent
    )
}
