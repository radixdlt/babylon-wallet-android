package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
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
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppSettingsContent(
        onBackClick = onBackClick,
        appSettings = state.settings,
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        onDeveloperModeToggled = viewModel::onDeveloperModeToggled
    )
}

@Composable
private fun AppSettingsContent(
    onBackClick: () -> Unit,
    appSettings: ImmutableSet<SettingsItem.AppSettings>,
    modifier: Modifier = Modifier,
    onDeveloperModeToggled: (Boolean) -> Unit
) {
    Column(
        modifier = modifier.background(RadixTheme.colors.defaultBackground),
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.appSettings_title),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Divider(color = RadixTheme.colors.gray5)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            appSettings.forEachIndexed { index, settingsItem ->
                item {
                    when (settingsItem) {
                        is SettingsItem.AppSettings.DeveloperMode -> {
                            SwitchSettingsItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(all = RadixTheme.dimensions.paddingDefault),
                                titleRes = settingsItem.descriptionRes(),
                                subtitleRes = settingsItem.subtitleRes(),
                                icon = settingsItem.getIcon(),
                                checked = settingsItem.enabled,
                                onCheckedChange = onDeveloperModeToggled
                            )
                        }
                    }

                    if (index < appSettings.count() - 1) {
                        Divider(color = RadixTheme.colors.gray5)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenWithoutActiveConnectionPreview() {
    RadixWalletTheme {
        AppSettingsContent(
            onBackClick = {},
            appSettings = persistentSetOf(
                SettingsItem.AppSettings.DeveloperMode(false),
            ),
            onDeveloperModeToggled = {}
        )
    }
}
