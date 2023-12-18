package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.collections.immutable.ImmutableSet

@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel,
    onAppSettingItemClick: (SettingsItem.AppSettingsItem) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppSettingsContent(
        modifier = modifier,
        appSettings = state.settings,
        onAppSettingItemClick = onAppSettingItemClick,
        onDeveloperModeToggled = viewModel::onDeveloperModeToggled,
        onBackClick = onBackClick,
        onCrashReportingToggled = viewModel::onCrashReportingToggled
    )
}

@Composable
private fun AppSettingsContent(
    modifier: Modifier = Modifier,
    appSettings: ImmutableSet<SettingsItem.AppSettingsItem>,
    onAppSettingItemClick: (SettingsItem.AppSettingsItem) -> Unit,
    onDeveloperModeToggled: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onCrashReportingToggled: (Boolean) -> Unit,
) {
    var crashReportingPromptVisible by remember { mutableStateOf(false) }
    if (crashReportingPromptVisible) {
        BasicPromptAlertDialog(
            finish = { accepted ->
                if (accepted) {
                    onCrashReportingToggled(true)
                }
                crashReportingPromptVisible = false
            },
            title = stringResource(id = R.string.appSettings_crashReporting_title),
            text = stringResource(id = R.string.appSettings_crashReporting_subtitle)
        )
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.appSettings_title),
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
//            item { // TODO when we can really customize it
//                Text(
//                    modifier = Modifier.padding(all = RadixTheme.dimensions.paddingDefault),
//                    text = stringResource(id = R.string.appSettings_subtitle),
//                    style = RadixTheme.typography.body1HighImportance,
//                    color = RadixTheme.colors.gray2
//                )
//            }
                appSettings.forEach { appSettingsItem ->
                    item {
                        when (appSettingsItem) {
                            is SettingsItem.AppSettingsItem.DeveloperMode -> {
                                SwitchSettingsItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(all = RadixTheme.dimensions.paddingDefault),
                                    titleRes = appSettingsItem.descriptionRes(),
                                    subtitleRes = R.string.appSettings_developerMode_subtitle, // appSettingsItem.subtitleRes(),
                                    iconResource = appSettingsItem.getIcon(),
                                    checked = appSettingsItem.enabled,
                                    onCheckedChange = onDeveloperModeToggled
                                )
                            }

                            is SettingsItem.AppSettingsItem.CrashReporting -> {
                                SwitchSettingsItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(all = RadixTheme.dimensions.paddingDefault),
                                    titleRes = appSettingsItem.descriptionRes(),
                                    iconResource = appSettingsItem.getIcon(),
                                    checked = appSettingsItem.enabled,
                                    onCheckedChange = { selected ->
                                        if (selected) {
                                            crashReportingPromptVisible = true
                                        } else {
                                            onCrashReportingToggled(false)
                                        }
                                    }
                                )
                            }

                            SettingsItem.AppSettingsItem.EntityHiding -> {
                                DefaultSettingsItem(
                                    title = stringResource(id = appSettingsItem.descriptionRes()),
                                    icon = appSettingsItem.getIcon(),
                                    onClick = {
                                        onAppSettingItemClick(appSettingsItem)
                                    },
                                    subtitle = stringResource(id = R.string.appSettings_entityHiding_subtitle)
                                )
                            }

                            else -> {
                                DefaultSettingsItem(
                                    title = stringResource(id = appSettingsItem.descriptionRes()),
                                    icon = appSettingsItem.getIcon(),
                                    onClick = {
                                        onAppSettingItemClick(appSettingsItem)
                                    }
                                )
                            }
                        }
                        HorizontalDivider(color = RadixTheme.colors.gray5)
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
        AppSettingsContent(
            modifier = Modifier,
            appSettings = AppSettingsUiState.default.settings,
            onAppSettingItemClick = {},
            onDeveloperModeToggled = {},
            onBackClick = {},
            onCrashReportingToggled = {}
        )
    }
}
