package com.babylon.wallet.android.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(
        modifier = modifier,
        appSettings = state.settings,
        onSettingClick = onSettingClick,
        onHideImportOlympiaWalletSettingBox = viewModel::hideImportOlympiaWalletSettingBox,
        onBackClick = onBackClick
    )
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    appSettings: ImmutableList<SettingsItem.TopLevelSettings>,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    onHideImportOlympiaWalletSettingBox: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.settings_title),
                onBackClick = onBackClick,
                contentColor = RadixTheme.colors.gray1,
                titleIcon = {
                    Icon(
                        painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_settings),
                        tint = RadixTheme.colors.gray1,
                        contentDescription = "settings gear"
                    )
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            Divider(color = RadixTheme.colors.gray4)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                appSettings.forEach { settingsItem ->
                    when (settingsItem) {
                        SettingsItem.TopLevelSettings.LinkToConnector -> {
                            item {
                                ConnectorSettingBox(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(RadixTheme.colors.gray5)
                                        .padding(RadixTheme.dimensions.paddingDefault),
                                    onSettingClick = onSettingClick,
                                    settingsItem = settingsItem
                                )
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
                            }
                        }

                        SettingsItem.TopLevelSettings.ImportOlympiaWallet -> {
                            item {
                                ImportOlympiaWalletSettingBox(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(RadixTheme.colors.gray5)
                                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                                        .padding(bottom = RadixTheme.dimensions.paddingDefault),
                                    onSettingClick = onSettingClick,
                                    settingsItem = settingsItem,
                                    onDismissClick = onHideImportOlympiaWalletSettingBox
                                )
                            }
                        }

                        else -> {
                            item {
                                DefaultSettingsItem(
                                    onClick = {
                                        onSettingClick(settingsItem)
                                    },
                                    icon = settingsItem.getIcon(),
                                    title = stringResource(id = settingsItem.descriptionRes()),
                                    showNotificationDot = (settingsItem as? SettingsItem.TopLevelSettings.AppSettings)
                                        ?.showNotificationWarning ?: false
                                )
                            }
                            item {
                                Divider(color = RadixTheme.colors.gray5)
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Text(
                        text = stringResource(
                            R.string.settings_appVersion,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE.toString()
                        ),
                        style = RadixTheme.typography.body2Link,
                        color = RadixTheme.colors.gray2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                }
            }
        }
    }
}

@Composable
private fun ConnectorSettingBox(
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    settingsItem: SettingsItem.TopLevelSettings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Image(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_connector),
            contentDescription = null
        )
        Text(
            text = stringResource(R.string.settings_linkToConnectorHeader_title),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.settings_linkToConnectorHeader_subtitle),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray2,
            textAlign = TextAlign.Center
        )
        RadixSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(R.string.settings_linkToConnectorHeader_linkToConnector),
            onClick = {
                onSettingClick(settingsItem)
            },
            containerColor = RadixTheme.colors.gray3,
            contentColor = RadixTheme.colors.gray1,
            icon = {
                Icon(
                    painter = painterResource(
                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner
                    ),
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun ImportOlympiaWalletSettingBox(
    modifier: Modifier = Modifier,
    settingsItem: SettingsItem.TopLevelSettings,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    onDismissClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = CenterVertically
        ) {
            IconButton(onClick = onDismissClick) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "dismiss"
                )
            }
        }
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Text(
                text = stringResource(id = R.string.settings_importFromLegacyWalletHeader_title),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.settings_importFromLegacyWalletHeader_subtitle),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2,
                textAlign = TextAlign.Center
            )
            RadixSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.settings_importFromLegacyWalletHeader_importLegacyAccounts),
                onClick = {
                    onSettingClick(settingsItem)
                },
                containerColor = RadixTheme.colors.gray3,
                contentColor = RadixTheme.colors.gray1,
                icon = {
                    Icon(
                        painter = painterResource(
                            id = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner
                        ),
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenWithoutActiveConnectionPreview() {
    RadixWalletTheme {
        SettingsContent(
            appSettings = persistentListOf(
                SettingsItem.TopLevelSettings.LinkToConnector,
                SettingsItem.TopLevelSettings.ImportOlympiaWallet,
                SettingsItem.TopLevelSettings.AuthorizedDapps,
                SettingsItem.TopLevelSettings.Personas,
                SettingsItem.TopLevelSettings.AccountSecurityAndSettings,
                SettingsItem.TopLevelSettings.AppSettings(showNotificationWarning = true)
            ),
            onSettingClick = {},
            onHideImportOlympiaWalletSettingBox = {},
            onBackClick = {}
        )
    }
}
