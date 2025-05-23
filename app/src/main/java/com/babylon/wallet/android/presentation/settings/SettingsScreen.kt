@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.DependencyInformation
import com.radixdlt.sargon.extensions.string
import kotlinx.collections.immutable.toPersistentList

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(
        modifier = modifier,
        state = state,
        onSettingClick = onSettingClick,
    )
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.walletSettings_title),
                    onBackClick = {},
                    backIconType = BackIconType.None,
                    contentColor = RadixTheme.colors.text,
                    windowInsets = WindowInsets.statusBarsAndBanner,
                    containerColor = RadixTheme.colors.background
                )

                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = padding
        ) {
            state.settings.forEach { settingsItem ->
                when (settingsItem) {
                    SettingsUiItem.Spacer -> {
                        item {
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
                        }
                    }

                    is SettingsUiItem.Settings -> {
                        item {
                            DefaultSettingsItem(
                                onClick = {
                                    onSettingClick(settingsItem.item)
                                },
                                subtitle = stringResource(id = settingsItem.item.subtitleRes()),
                                leadingIconRes = settingsItem.item.getIcon(),
                                title = stringResource(id = settingsItem.item.titleRes()),
                                warnings = when (val item = settingsItem.item) {
                                    is SettingsItem.TopLevelSettings.SecurityCenter -> {
                                        if (item.securityProblems.isNotEmpty()) {
                                            item.securityProblems.map { it.toProblemHeading() }.toPersistentList()
                                        } else {
                                            null
                                        }
                                    }

                                    is SettingsItem.TopLevelSettings.Personas -> {
                                        personaWarnings(item)
                                    }

                                    else -> null
                                }
                            )
                        }
                        item {
                            HorizontalDivider(color = RadixTheme.colors.divider)
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
                    color = RadixTheme.colors.textSecondary,
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
                    color = RadixTheme.colors.text
                ).toSpanStyle()
            ) {
                append(dependencyVersion)
            }
        },
        style = RadixTheme.typography.body1Regular,
        color = RadixTheme.colors.textSecondary,
        textAlign = TextAlign.Start
    )
}

@Composable
fun DependencyInformation.displayable(): String = remember(this) {
    string.takeLast(7)
}

@Composable
fun personaWarnings(personaItem: SettingsItem.TopLevelSettings.Personas) = mutableListOf<String>().apply {
    personaItem.isCloudBackupNotWorking?.toText()?.let {
        add(it)
    }

    if (personaItem.isBackupNeeded) {
        add(stringResource(R.string.securityProblems_no3_walletSettingsPersonas))
    }

    if (personaItem.isRecoveryNeeded) {
        add(stringResource(R.string.securityProblems_no9_walletSettingsPersonas))
    }
}.toPersistentList()

@Preview(showBackground = true)
@Composable
fun SettingsWithoutActiveConnectionPreview() {
    RadixWalletTheme {
        SettingsContent(
            state = SettingsUiState(
                settings = listOf(
                    SettingsItem.TopLevelSettings.SecurityCenter(),
                    SettingsItem.TopLevelSettings.Personas(
                        isBackupNeeded = false,
                        isRecoveryNeeded = false
                    ),
                    SettingsItem.TopLevelSettings.ApprovedDapps,
                    SettingsItem.TopLevelSettings.LinkedConnectors,
                    SettingsItem.TopLevelSettings.Preferences,
                    SettingsItem.TopLevelSettings.Troubleshooting
                ).map { SettingsUiItem.Settings(it) }.toPersistentList()
            ),
            onSettingClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsWithSecurityProblem5Preview() {
    RadixWalletTheme {
        val cloudBackupProblem = SecurityProblem.CloudBackupNotWorking.ServiceError(isAnyActivePersonaAffected = false)
        SettingsContent(
            state = SettingsUiState(
                settings = listOf(
                    SettingsItem.TopLevelSettings.SecurityCenter(
                        securityProblems = setOf(cloudBackupProblem)
                    ),
                    SettingsItem.TopLevelSettings.Personas(
                        isCloudBackupNotWorking = cloudBackupProblem,
                        isBackupNeeded = false,
                        isRecoveryNeeded = false
                    ),
                    SettingsItem.TopLevelSettings.ApprovedDapps,
                    SettingsItem.TopLevelSettings.LinkedConnectors,
                    SettingsItem.TopLevelSettings.Preferences,
                    SettingsItem.TopLevelSettings.Troubleshooting
                ).map { SettingsUiItem.Settings(it) }.toPersistentList()
            ),
            onSettingClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsWithSecurityProblems3And6Preview() {
    RadixWalletTheme {
        val cloudBackupProblem = SecurityProblem.CloudBackupNotWorking.Disabled(
            isAnyActivePersonaAffected = true,
            hasManualBackup = true
        )
        SettingsContent(
            state = SettingsUiState(
                settings = listOf(
                    SettingsItem.TopLevelSettings.SecurityCenter(
                        securityProblems = setOf(
                            cloudBackupProblem,
                            SecurityProblem.EntitiesNotRecoverable(
                                accountsNeedBackup = 3,
                                personasNeedBackup = 4,
                                hiddenAccountsNeedBackup = 0,
                                hiddenPersonasNeedBackup = 0
                            )
                        )
                    ),
                    SettingsItem.TopLevelSettings.Personas(
                        isCloudBackupNotWorking = cloudBackupProblem,
                        isBackupNeeded = true,
                        isRecoveryNeeded = false
                    ),
                    SettingsItem.TopLevelSettings.ApprovedDapps,
                    SettingsItem.TopLevelSettings.LinkedConnectors,
                    SettingsItem.TopLevelSettings.Preferences,
                    SettingsItem.TopLevelSettings.Troubleshooting
                ).map { SettingsUiItem.Settings(it) }.toPersistentList()
            ),
            onSettingClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsWithSecurityProblems7And9Preview() {
    RadixWalletTheme {
        val cloudBackupProblem = SecurityProblem.CloudBackupNotWorking.Disabled(
            isAnyActivePersonaAffected = true,
            hasManualBackup = true
        )
        SettingsContent(
            state = SettingsUiState(
                settings = listOf(
                    SettingsItem.TopLevelSettings.SecurityCenter(
                        securityProblems = setOf(
                            cloudBackupProblem,
                            SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = true)
                        )
                    ),
                    SettingsItem.TopLevelSettings.Personas(
                        isCloudBackupNotWorking = cloudBackupProblem,
                        isBackupNeeded = false,
                        isRecoveryNeeded = true
                    ),
                    SettingsItem.TopLevelSettings.ApprovedDapps,
                    SettingsItem.TopLevelSettings.LinkedConnectors,
                    SettingsItem.TopLevelSettings.Preferences,
                    SettingsItem.TopLevelSettings.Troubleshooting
                ).map { SettingsUiItem.Settings(it) }.toPersistentList()
            ),
            onSettingClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsWithSecurityProblems3And9Preview() {
    RadixWalletTheme {
        SettingsContent(
            state = SettingsUiState(
                settings = listOf(
                    SettingsItem.TopLevelSettings.SecurityCenter(
                        securityProblems = setOf(
                            SecurityProblem.EntitiesNotRecoverable(
                                accountsNeedBackup = 3,
                                personasNeedBackup = 2,
                                hiddenAccountsNeedBackup = 0,
                                hiddenPersonasNeedBackup = 0
                            ),
                            SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = true)
                        )
                    ),
                    SettingsItem.TopLevelSettings.Personas(
                        isBackupNeeded = true,
                        isRecoveryNeeded = true
                    ),
                    SettingsItem.TopLevelSettings.ApprovedDapps,
                    SettingsItem.TopLevelSettings.LinkedConnectors,
                    SettingsItem.TopLevelSettings.Preferences,
                    SettingsItem.TopLevelSettings.Troubleshooting
                ).map { SettingsUiItem.Settings(it) }.toPersistentList()
            ),
            onSettingClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsWithSecurityProblems3And5And9Preview() {
    RadixWalletTheme {
        val cloudBackupProblem = SecurityProblem.CloudBackupNotWorking.ServiceError(isAnyActivePersonaAffected = true)
        SettingsContent(
            state = SettingsUiState(
                settings = listOf(
                    SettingsItem.TopLevelSettings.SecurityCenter(
                        securityProblems = setOf(
                            cloudBackupProblem,
                            SecurityProblem.EntitiesNotRecoverable(
                                accountsNeedBackup = 4,
                                personasNeedBackup = 2,
                                hiddenAccountsNeedBackup = 0,
                                hiddenPersonasNeedBackup = 0
                            ),
                            SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = true)
                        )
                    ),
                    SettingsItem.TopLevelSettings.Personas(
                        isCloudBackupNotWorking = cloudBackupProblem,
                        isBackupNeeded = true,
                        isRecoveryNeeded = true
                    ),
                    SettingsItem.TopLevelSettings.ApprovedDapps,
                    SettingsItem.TopLevelSettings.LinkedConnectors,
                    SettingsItem.TopLevelSettings.Preferences,
                    SettingsItem.TopLevelSettings.Troubleshooting
                ).map { SettingsUiItem.Settings(it) }.toPersistentList()
            ),
            onSettingClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsWithSecurityProblems3And5And9AndOnlyHiddenPersonasPreview() {
    RadixWalletTheme {
        val cloudBackupProblem = SecurityProblem.CloudBackupNotWorking.ServiceError(isAnyActivePersonaAffected = false)
        SettingsContent(
            state = SettingsUiState(
                settings = listOf(
                    SettingsItem.TopLevelSettings.SecurityCenter(
                        securityProblems = setOf(
                            cloudBackupProblem,
                            SecurityProblem.EntitiesNotRecoverable(
                                accountsNeedBackup = 4,
                                personasNeedBackup = 0,
                                hiddenAccountsNeedBackup = 0,
                                hiddenPersonasNeedBackup = 2
                            ),
                            SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = true)
                        )
                    ),
                    SettingsItem.TopLevelSettings.Personas(
                        isCloudBackupNotWorking = cloudBackupProblem,
                        isBackupNeeded = false,
                        isRecoveryNeeded = false
                    ),
                    SettingsItem.TopLevelSettings.ApprovedDapps,
                    SettingsItem.TopLevelSettings.LinkedConnectors,
                    SettingsItem.TopLevelSettings.Preferences,
                    SettingsItem.TopLevelSettings.Troubleshooting
                ).map { SettingsUiItem.Settings(it) }.toPersistentList()
            ),
            onSettingClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsWithSecurityProblems7And9AndOnlyHiddenPersonasPreview() {
    RadixWalletTheme {
        val cloudBackupProblem = SecurityProblem.CloudBackupNotWorking.Disabled(
            isAnyActivePersonaAffected = false,
            hasManualBackup = true
        )
        SettingsContent(
            state = SettingsUiState(
                settings = listOf(
                    SettingsItem.TopLevelSettings.SecurityCenter(
                        securityProblems = setOf(
                            cloudBackupProblem,
                            SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = false)
                        )
                    ),
                    SettingsItem.TopLevelSettings.Personas(
                        isCloudBackupNotWorking = cloudBackupProblem,
                        isBackupNeeded = false,
                        isRecoveryNeeded = false
                    ),
                    SettingsItem.TopLevelSettings.ApprovedDapps,
                    SettingsItem.TopLevelSettings.LinkedConnectors,
                    SettingsItem.TopLevelSettings.Preferences,
                    SettingsItem.TopLevelSettings.Troubleshooting
                ).map { SettingsUiItem.Settings(it) }.toPersistentList()
            ),
            onSettingClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsWithSecurityProblems3And5And9AndOnlyHiddenEntitiesPreview() {
    RadixWalletTheme {
        val cloudBackupProblem = SecurityProblem.CloudBackupNotWorking.ServiceError(isAnyActivePersonaAffected = false)
        SettingsContent(
            state = SettingsUiState(
                settings = listOf(
                    SettingsItem.TopLevelSettings.SecurityCenter(
                        securityProblems = setOf(
                            cloudBackupProblem,
                            SecurityProblem.EntitiesNotRecoverable(
                                accountsNeedBackup = 0,
                                personasNeedBackup = 0,
                                hiddenAccountsNeedBackup = 4,
                                hiddenPersonasNeedBackup = 2
                            ),
                            SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = false)
                        )
                    ),
                    SettingsItem.TopLevelSettings.Personas(
                        isCloudBackupNotWorking = cloudBackupProblem,
                        isBackupNeeded = false,
                        isRecoveryNeeded = false
                    ),
                    SettingsItem.TopLevelSettings.ApprovedDapps,
                    SettingsItem.TopLevelSettings.LinkedConnectors,
                    SettingsItem.TopLevelSettings.Preferences,
                    SettingsItem.TopLevelSettings.Troubleshooting
                ).map { SettingsUiItem.Settings(it) }.toPersistentList()
            ),
            onSettingClick = {},
        )
    }
}
