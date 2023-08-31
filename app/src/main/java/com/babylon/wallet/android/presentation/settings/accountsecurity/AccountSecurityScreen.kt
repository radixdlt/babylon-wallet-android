package com.babylon.wallet.android.presentation.settings.accountsecurity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableSet

@Composable
fun AccountSecurityScreen(
    viewModel: AccountSecurityViewModel,
    onAccountSecuritySettingItemClick: (SettingsItem.AccountSecurityAndSettingsItem) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountSecurityContent(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        appSettings = state.settings,
        onAccountSecuritySettingItemClick = onAccountSecuritySettingItemClick,
        onBackClick = onBackClick,
    )
}

@Composable
private fun AccountSecurityContent(
    modifier: Modifier = Modifier,
    appSettings: ImmutableSet<SettingsItem.AccountSecurityAndSettingsItem>,
    onAccountSecuritySettingItemClick: (SettingsItem.AccountSecurityAndSettingsItem) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = modifier.background(RadixTheme.colors.defaultBackground),
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.accountSettings_accountSecurity),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Divider(color = RadixTheme.colors.gray5)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            appSettings.forEachIndexed { index, accountSecurityAndSettingsItem ->
                item {
                    DefaultSettingsItem(
                        settingsItem = accountSecurityAndSettingsItem,
                        onClick = {
                            onAccountSecuritySettingItemClick(accountSecurityAndSettingsItem)
                        }
                    )

                    if (index < appSettings.count() - 1) {
                        Divider(color = RadixTheme.colors.gray5)
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultSettingsItem(
    settingsItem: SettingsItem.AccountSecurityAndSettingsItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(RadixTheme.colors.defaultBackground)
            .throttleClickable(onClick = onClick)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        settingsItem.getIcon()?.let {
            Icon(painter = painterResource(id = it), contentDescription = null)
        }
        Text(
            text = stringResource(id = settingsItem.descriptionRes()),
            style = RadixTheme.typography.body2Header,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.gray1
        )
    }
}
