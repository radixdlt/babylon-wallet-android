package com.babylon.wallet.android.presentation.settings.accountsecurity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem.AccountSecurityAndSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
fun AccountSecurityScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountSecurityViewModel,
    onAccountSecuritySettingItemClick: (AccountSecurityAndSettingsItem) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountSecurityContent(
        modifier = modifier,
        appSettings = state.settings,
        onAccountSecuritySettingItemClick = onAccountSecuritySettingItemClick,
        onBackClick = onBackClick,
    )
}

@Composable
private fun AccountSecurityContent(
    modifier: Modifier = Modifier,
    appSettings: ImmutableSet<AccountSecurityAndSettingsItem>,
    onAccountSecuritySettingItemClick: (AccountSecurityAndSettingsItem) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.settings_accountSecurityAndSettings),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.gray5)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                appSettings.forEachIndexed { index, accountSecurityAndSettingsItem ->
                    item {
                        DefaultSettingsItem(
                            title = stringResource(id = accountSecurityAndSettingsItem.descriptionRes()),
                            icon = accountSecurityAndSettingsItem.getIcon(),
                            onClick = {
                                onAccountSecuritySettingItemClick(accountSecurityAndSettingsItem)
                            },
                            subtitle =
                            if (accountSecurityAndSettingsItem is AccountSecurityAndSettingsItem.DepositGuarantees) {
                                stringResource(id = R.string.settings_depositGuarantees_subtitle)
                            } else {
                                null
                            }
                        )
                        HorizontalDivider(color = RadixTheme.colors.gray5)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountSecurityScreenPreview() {
    RadixWalletTheme {
        AccountSecurityContent(
            modifier = Modifier,
            appSettings = persistentSetOf(),
            onAccountSecuritySettingItemClick = {},
            onBackClick = {}
        )
    }
}
