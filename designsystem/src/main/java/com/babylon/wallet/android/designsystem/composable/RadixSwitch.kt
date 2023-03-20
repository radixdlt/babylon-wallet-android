package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

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
        thumbContent = {
            Icon(
                modifier = Modifier.padding(4.dp),
                painter = painterResource(id = R.drawable.ic_check_bold),
                contentDescription = null
            )
        },
        colors = RadixSwitchDefaults.colors(),
    )
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun RadixSwitchPreview() {
    RadixWalletTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
        checkedThumbColor = RadixTheme.colors.white,
        checkedTrackColor = RadixTheme.colors.gray1,
        checkedBorderColor = RadixTheme.colors.gray1,
        checkedIconColor = RadixTheme.colors.gray1,
        uncheckedThumbColor = RadixTheme.colors.white,
        uncheckedTrackColor = RadixTheme.colors.gray4,
        uncheckedBorderColor = RadixTheme.colors.gray4,
        uncheckedIconColor = RadixTheme.colors.white,
        disabledCheckedThumbColor = RadixTheme.colors.white,
        disabledCheckedTrackColor = RadixTheme.colors.gray4,
        disabledCheckedBorderColor = RadixTheme.colors.gray4,
        disabledCheckedIconColor = RadixTheme.colors.gray4,
        disabledUncheckedThumbColor = RadixTheme.colors.white,
        disabledUncheckedTrackColor = RadixTheme.colors.gray4,
        disabledUncheckedBorderColor = RadixTheme.colors.gray4,
        disabledUncheckedIconColor = RadixTheme.colors.white
    )
}
