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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.radixdlt.sargon.DependencyInformation
import com.radixdlt.sargon.extensions.string
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
        state = state,
        onSettingClick = onSettingClick,
        onHideImportOlympiaWalletSettingBox = viewModel::hideImportOlympiaWalletSettingBox,
        onBackClick = onBackClick
    )
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
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
            HorizontalDivider(color = RadixTheme.colors.gray4)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                state.settings.forEach { settingsItem ->
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

                        is SettingsItem.TopLevelSettings.Personas -> {
                            item {
                                DefaultSettingsItem(
                                    title = stringResource(id = settingsItem.descriptionRes()),
                                    subtitleView = if (settingsItem.showBackupSecurityPrompt) {
                                        { NotBackedUpPersonasWarning(Modifier.fillMaxWidth()) }
                                    } else {
                                        null
                                    },
                                    iconView = settingsItem.getIcon()?.let { iconRes ->
                                        {
                                            Icon(
                                                modifier = Modifier.size(24.dp),
                                                painter = painterResource(id = iconRes),
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSettingClick(settingsItem)
                                    }
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
                                    showNotificationDot = (settingsItem as? SettingsItem.TopLevelSettings.AccountSecurityAndSettings)
                                        ?.showNotificationWarning ?: false
                                )
                            }
                            item {
                                HorizontalDivider(color = RadixTheme.colors.gray5)
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

                state.debugBuildInformation?.let { buildInfo ->
                    item {
                        DebugBuildInformation(buildInfo = buildInfo)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotBackedUpPersonasWarning(modifier: Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
        verticalAlignment = CenterVertically
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error),
            contentDescription = null,
            tint = RadixTheme.colors.orange1
        )
        Text(
            text = "Write down main seed phrase", // TODO R.string.settings_personas_seedPhraseWarning),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.orange1
        )
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
            leadingContent = {
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
                text = stringResource(id = R.string.accountSecuritySettings_importFromLegacyWallet_title),
                onClick = {
                    onSettingClick(settingsItem)
                },
                containerColor = RadixTheme.colors.gray3,
                contentColor = RadixTheme.colors.gray1,
                leadingContent = {
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

@Composable
private fun DebugBuildInformation(
    modifier: Modifier = Modifier,
    buildInfo: DebugBuildInformation
) {
    Column(
        modifier = modifier.padding(
            vertical = RadixTheme.dimensions.paddingLarge,
            horizontal = RadixTheme.dimensions.paddingDefault
        )
    ) {
        VersionInformation(
            dependencyName = "Sargon Version",
            dependencyVersion = buildInfo.sargonInfo.sargonVersion
        )
        VersionInformation(
            dependencyName = "RET",
            dependencyVersion = "#${buildInfo.sargonInfo.dependencies.radixEngineToolkit.displayable()}"
        )
        VersionInformation(
            dependencyName = "Scrypto",
            dependencyVersion = "#${buildInfo.sargonInfo.dependencies.scryptoRadixEngine.displayable()}"
        )
        VersionInformation(
            dependencyName = "SigServer",
            dependencyVersion = buildInfo.SIGNALING_SERVER
        )
    }
}

@Composable
private fun VersionInformation(
    modifier: Modifier = Modifier,
    dependencyName: String,
    dependencyVersion: String
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = buildAnnotatedString {
            append("$dependencyName: ")
            withStyle(
                TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = RadixTheme.colors.gray1
                ).toSpanStyle()
            ) {
                append(dependencyVersion)
            }
        },
        style = RadixTheme.typography.body1Regular,
        color = RadixTheme.colors.gray2,
        textAlign = TextAlign.Start
    )
}

@Composable
fun DependencyInformation.displayable(): String = remember(this) {
    string.takeLast(7)
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenWithoutActiveConnectionPreview() {
    RadixWalletTheme {
        SettingsContent(
            state = SettingsUiState(
                persistentListOf(
                    SettingsItem.TopLevelSettings.LinkToConnector,
                    SettingsItem.TopLevelSettings.ImportOlympiaWallet,
                    SettingsItem.TopLevelSettings.AuthorizedDapps,
                    SettingsItem.TopLevelSettings.Personas(),
                    SettingsItem.TopLevelSettings.AccountSecurityAndSettings(showNotificationWarning = true),
                    SettingsItem.TopLevelSettings.AppSettings
                )
            ),
            onSettingClick = {},
            onHideImportOlympiaWalletSettingBox = {},
            onBackClick = {}
        )
    }
}
