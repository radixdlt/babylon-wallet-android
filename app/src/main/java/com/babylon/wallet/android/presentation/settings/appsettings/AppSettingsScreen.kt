package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.NotBackedUpWarning
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
    )
}

@Composable
private fun AppSettingsContent(
    modifier: Modifier = Modifier,
    appSettings: ImmutableSet<SettingsItem.AppSettingsItem>,
    onAppSettingItemClick: (SettingsItem.AppSettingsItem) -> Unit,
    onDeveloperModeToggled: (Boolean) -> Unit,
    onBackClick: () -> Unit,
) {
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
            Divider(color = RadixTheme.colors.gray5)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
//            item { // TODO when we can really customize it
//                Text(
//                    modifier = Modifier.padding(all = RadixTheme.dimensions.paddingDefault),
//                    text = stringResource(id = R.string.appSettings_subtitle),
//                    style = RadixTheme.typography.body1HighImportance,
//                    color = RadixTheme.colors.gray2
//                )
//            }
                appSettings.forEachIndexed { index, appSettingsItem ->
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
                                Divider(color = RadixTheme.colors.gray5)
                            }

                            else -> {
                                if (appSettingsItem is SettingsItem.AppSettingsItem.Backups) {
                                    DefaultSettingsItem(
                                        modifier = modifier,
                                        title = stringResource(id = appSettingsItem.descriptionRes()),
                                        subtitleView = {
                                            NotBackedUpWarning(backupState = appSettingsItem.backupState)
                                        },
                                        iconView = appSettingsItem.getIcon()?.let { iconRes ->
                                            {
                                                Icon(
                                                    modifier = Modifier.size(24.dp),
                                                    painter = painterResource(id = iconRes),
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        onClick = {
                                            onAppSettingItemClick(appSettingsItem)
                                        }
                                    )
                                } else {
                                    DefaultSettingsItem(
                                        title = stringResource(id = appSettingsItem.descriptionRes()),
                                        icon = appSettingsItem.getIcon(),
                                        onClick = {
                                            onAppSettingItemClick(appSettingsItem)
                                        }
                                    )
                                }
                                Divider(color = RadixTheme.colors.gray5)
                            }
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
        AppSettingsContent(
            modifier = Modifier,
            appSettings = AppSettingsUiState.default.settings,
            onAppSettingItemClick = {},
            onDeveloperModeToggled = {},
            onBackClick = {}
        )
    }
}
