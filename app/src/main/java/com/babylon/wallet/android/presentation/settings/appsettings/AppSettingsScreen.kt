package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
    appSettings: ImmutableList<SettingsItem.AppSettings>,
    modifier: Modifier = Modifier,
    onDeveloperModeToggled: (Boolean) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.app_settings),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Divider(color = RadixTheme.colors.gray5)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(R.string.customize_the_app_appearance),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                Divider(color = RadixTheme.colors.gray5)
            }
            appSettings.forEach { settingsItem ->
                when (settingsItem) {
                    is SettingsItem.AppSettings.DeveloperMode -> {
                        item {
                            SwitchSettingsItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .background(RadixTheme.colors.defaultBackground)
                                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                titleRes = settingsItem.descriptionRes(),
                                subtitleRes = settingsItem.subtitleRes(),
                                icon = settingsItem.getIcon(),
                                checked = settingsItem.enabled,
                                onCheckedChange = onDeveloperModeToggled
                            )
                        }
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
            appSettings = persistentListOf(
                SettingsItem.AppSettings.DeveloperMode(false),
            ),
            onDeveloperModeToggled = {}
        )
    }
}
