package com.babylon.wallet.android.presentation.settings.preferences

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.collections.immutable.ImmutableSet

@Composable
fun WalletPreferencesScreen(
    viewModel: WalletPreferencesViewModel,
    onWalletPreferenceItemClick: (SettingsItem.WalletPreferences) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    WalletPreferencesContent(
        modifier = modifier.fillMaxSize(),
        walletPreferences = state.settings,
        onWalletPreferenceItemClick = onWalletPreferenceItemClick,
        onDeveloperModeToggled = viewModel::onDeveloperModeToggled,
        onBackClick = onBackClick,
        onCrashReportingToggled = viewModel::onCrashReportingToggled,
        onAdvancedLockToggled = viewModel::onAdvancedLockToggled
    )
}

@Composable
private fun WalletPreferencesContent(
    modifier: Modifier = Modifier,
    walletPreferences: ImmutableSet<PreferencesUiItem>,
    onWalletPreferenceItemClick: (SettingsItem.WalletPreferences) -> Unit,
    onDeveloperModeToggled: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onCrashReportingToggled: (Boolean) -> Unit,
    onAdvancedLockToggled: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    var crashReportingPromptVisible by remember { mutableStateOf(false) }
    if (crashReportingPromptVisible) {
        BasicPromptAlertDialog(
            finish = { accepted ->
                if (accepted) {
                    onCrashReportingToggled(true)
                }
                crashReportingPromptVisible = false
            },
            titleText = stringResource(id = R.string.appSettings_crashReporting_title),
            messageText = stringResource(id = R.string.appSettings_crashReporting_subtitle)
        )
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.preferences_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )

                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
            }
            walletPreferences.forEach { walletPreferenceItem ->
                item {
                    when (walletPreferenceItem) {
                        PreferencesUiItem.AdvancedSection -> {
                            Text(
                                modifier = Modifier.padding(all = RadixTheme.dimensions.paddingDefault),
                                text = stringResource(id = R.string.preferences_advancedPreferences),
                                style = RadixTheme.typography.body1Link,
                                color = RadixTheme.colors.textSecondary
                            )
                        }

                        PreferencesUiItem.DisplaySection -> {
                            Text(
                                modifier = Modifier.padding(all = RadixTheme.dimensions.paddingDefault),
                                text = stringResource(id = R.string.preferences_displayPreferences),
                                style = RadixTheme.typography.body1Link,
                                color = RadixTheme.colors.textSecondary
                            )
                        }

                        is PreferencesUiItem.Preference -> {
                            when (val item = walletPreferenceItem.item) {
                                is SettingsItem.WalletPreferences.DeveloperMode -> {
                                    SwitchSettingsItem(
                                        modifier = Modifier
                                            .background(RadixTheme.colors.background)
                                            .fillMaxWidth()
                                            .padding(all = RadixTheme.dimensions.paddingDefault),
                                        titleRes = item.titleRes(),
                                        subtitleRes = R.string.appSettings_developerMode_subtitle, // appSettingsItem.subtitleRes(),
                                        iconResource = item.getIcon(),
                                        checked = item.enabled,
                                        onCheckedChange = onDeveloperModeToggled
                                    )
                                }

                                is SettingsItem.WalletPreferences.CrashReporting -> {
                                    SwitchSettingsItem(
                                        modifier = Modifier
                                            .background(RadixTheme.colors.background)
                                            .fillMaxWidth()
                                            .padding(all = RadixTheme.dimensions.paddingDefault),
                                        titleRes = item.titleRes(),
                                        iconResource = item.getIcon(),
                                        checked = item.enabled,
                                        onCheckedChange = { selected ->
                                            if (selected) {
                                                crashReportingPromptVisible = true
                                            } else {
                                                onCrashReportingToggled(false)
                                            }
                                        }
                                    )
                                }

                                is SettingsItem.WalletPreferences.AdvancedLock -> {
                                    SwitchSettingsItem(
                                        modifier = Modifier
                                            .background(RadixTheme.colors.background)
                                            .fillMaxWidth()
                                            .padding(all = RadixTheme.dimensions.paddingDefault),
                                        titleRes = item.titleRes(),
                                        subtitleRes = item.subtitleRes(),
                                        iconResource = item.getIcon(),
                                        checked = item.enabled,
                                        onCheckedChange = { checked ->
                                            context.biometricAuthenticate { result ->
                                                if (result == BiometricAuthenticationResult.Succeeded) {
                                                    onAdvancedLockToggled(checked)
                                                } // else do nothing
                                            }
                                        }
                                    )
                                    HorizontalDivider(color = RadixTheme.colors.divider)
                                }

                                else -> {
                                    DefaultSettingsItem(
                                        modifier = Modifier
                                            .background(RadixTheme.colors.background)
                                            .fillMaxWidth(),
                                        title = stringResource(id = item.titleRes()),
                                        leadingIconRes = item.getIcon(),
                                        subtitle = item.subtitleRes()
                                            ?.let { stringResource(id = it) },
                                        onClick = {
                                            onWalletPreferenceItemClick(item)
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = RadixTheme.colors.divider)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppSettingsScreenPreview() {
    RadixWalletTheme {
        WalletPreferencesContent(
            modifier = Modifier,
            walletPreferences = WalletPreferencesUiState.default.settings,
            onWalletPreferenceItemClick = {},
            onDeveloperModeToggled = {},
            onBackClick = {},
            onCrashReportingToggled = {},
            onAdvancedLockToggled = {}
        )
    }
}
