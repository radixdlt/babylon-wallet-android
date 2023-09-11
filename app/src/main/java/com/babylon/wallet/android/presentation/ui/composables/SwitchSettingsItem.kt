package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.designsystem.composable.RadixSwitch
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun SwitchSettingsItem(
    @StringRes titleRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    @StringRes subtitleRes: Int? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        icon?.invoke()
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(id = titleRes),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )
            subtitleRes?.let {
                Text(
                    text = stringResource(id = subtitleRes),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
            }
        }

        RadixSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SwitchSettingsItem(
    @StringRes titleRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    @StringRes subtitleRes: Int? = null,
) {
    SwitchSettingsItem(
        modifier = modifier,
        titleRes = titleRes,
        checked = checked,
        onCheckedChange = onCheckedChange,
        icon = iconResource?.let {
            {
                Icon(painter = painterResource(id = iconResource), contentDescription = null)
            }
        },
        subtitleRes = subtitleRes
    )
}
