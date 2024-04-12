package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
fun SecurityFactorsScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityFactorsViewModel,
    onSecurityFactorSettingItemClick: (SettingsItem.SecurityFactorsSettingsItem) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecurityFactorsContent(
        modifier = modifier.fillMaxSize(),
        securityFactorsSettings = state.settings,
        onSecurityFactorSettingItemClick = onSecurityFactorSettingItemClick,
        onBackClick = onBackClick,
    )
}

@Composable
private fun SecurityFactorsContent(
    modifier: Modifier = Modifier,
    securityFactorsSettings: ImmutableSet<SettingsItem.SecurityFactorsSettingsItem>,
    onSecurityFactorSettingItemClick: (SettingsItem.SecurityFactorsSettingsItem) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = "Security Factors", // TODO crowdin
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                Text(
                    text = "View and manage your security factors",
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray2,
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
                )
            }
            securityFactorsSettings.forEach { securityFactorsSettingsItem ->
                val lastItem = securityFactorsSettings.last() == securityFactorsSettingsItem
                item {
                    DefaultSettingsItem(
                        title = stringResource(id = securityFactorsSettingsItem.descriptionRes()),
                        subtitle = stringResource(id = securityFactorsSettingsItem.subtitleRes()),
                        icon = securityFactorsSettingsItem.getIcon(),
                        onClick = {
                            onSecurityFactorSettingItemClick(securityFactorsSettingsItem)
                        },
                        info = when (securityFactorsSettingsItem) {
                            is SettingsItem.SecurityFactorsSettingsItem.LedgerHardwareWallets -> {
                                "${securityFactorsSettingsItem.count} set" // TODO crowdin
                            }

                            is SettingsItem.SecurityFactorsSettingsItem.SeedPhrases -> {
                                "${securityFactorsSettingsItem.count} Seed Phrases" // TODO crowdin
                            }
                        },
                        warning = if (securityFactorsSettingsItem is SettingsItem.SecurityFactorsSettingsItem.SeedPhrases) {
                            "Enter your seed phrase to recover Accounts" // TODO crowdin
                        } else {
                            null
                        }
                    )
                    if (!lastItem) {
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
        SecurityFactorsContent(
            modifier = Modifier,
            securityFactorsSettings = persistentSetOf(),
            onSecurityFactorSettingItemClick = {},
            onBackClick = {}
        )
    }
}
