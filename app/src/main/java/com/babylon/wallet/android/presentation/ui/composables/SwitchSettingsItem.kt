package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSwitch
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun SwitchSettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    icon: @Composable (() -> Unit)? = null,
    subtitle: String? = null,
    subtitleTextColor: Color = RadixTheme.colors.text,
    isLoading: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        icon?.let {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                it()
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = RadixTheme.typography.body1Regular,
                    color = subtitleTextColor
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.height(28.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = RadixTheme.colors.icon
                )
            }
        } else {
            RadixSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SwitchSettingsItem(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    icon: @Composable (() -> Unit)? = null,
    @StringRes subtitleRes: Int? = null,
    subtitleTextColor: Color = RadixTheme.colors.text,
    isLoading: Boolean = false
) {
    SwitchSettingsItem(
        modifier = modifier,
        title = stringResource(id = titleRes),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        icon = icon,
        subtitle = subtitleRes?.let { stringResource(id = it) },
        subtitleTextColor = subtitleTextColor,
        isLoading = isLoading
    )
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
        title = stringResource(id = titleRes),
        checked = checked,
        onCheckedChange = onCheckedChange,
        iconResource = iconResource,
        subtitle = subtitleRes?.let { stringResource(id = it) }
    )
}

@Composable
fun SwitchSettingsItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    subtitle: String? = null,
) {
    SwitchSettingsItem(
        modifier = modifier,
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
        icon = iconResource?.let {
            {
                Icon(
                    painter = painterResource(id = iconResource),
                    contentDescription = null,
                    tint = RadixTheme.colors.icon
                )
            }
        },
        subtitle = subtitle
    )
}

@Preview(showBackground = true)
@Composable
private fun SwitchSettingsItemPreview() {
    RadixWalletPreviewTheme {
        SwitchSettingsItem(
            titleRes = R.string.configurationBackup_automated_toggleAndroid,
            checked = true,
            icon = {
                Icon(
                    painter = painterResource(id = DSR.ic_backup),
                    contentDescription = null
                )
            },
            onCheckedChange = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SwitchSettingsItemLoadingPreview() {
    RadixWalletPreviewTheme {
        SwitchSettingsItem(
            titleRes = R.string.configurationBackup_automated_toggleAndroid,
            checked = true,
            icon = {
                Icon(
                    painter = painterResource(id = DSR.ic_backup),
                    contentDescription = null
                )
            },
            onCheckedChange = { },
            isLoading = true
        )
    }
}
