package com.babylon.wallet.android.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onSettingClick: (SettingSectionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state
    SettingsContent(
        onBackClick = onBackClick,
        appSettings = state.settings,
        onSettingClick = onSettingClick,
        modifier = modifier
//            .systemBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    )
}

@Composable
private fun SettingsContent(
    onBackClick: () -> Unit,
    appSettings: ImmutableList<SettingSectionItem>,
    onSettingClick: (SettingSectionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.settings),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Column(
            Modifier.fillMaxSize()

        ) {
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                appSettings.forEach { settingsItem ->
                    when (settingsItem) {
                        SettingSectionItem.Connection -> {
                            item {
                                ConnectionSettingItem(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(RadixTheme.dimensions.paddingDefault)
                                    .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectDefault)
                                    .padding(RadixTheme.dimensions.paddingDefault),
                                    onSettingClick = onSettingClick, settingsItem = settingsItem)
                            }
                        }
                        SettingSectionItem.DeleteAll -> {
                            settingsItem.descriptionRes()?.let {
                                item {
                                    RadixSecondaryButton(modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingDefault),
                                        text = stringResource(id = it),
                                        onClick = {
                                            onSettingClick(settingsItem)
                                        },
                                        contentColor = RadixTheme.colors.red1)
                                }
                            }
                        }
                        else -> {
                            item {
                                DefaultSettingsItem(settingsItem = settingsItem,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .background(RadixTheme.colors.defaultBackground)
                                        .throttleClickable { onSettingClick(settingsItem) }
                                        .padding(horizontal = RadixTheme.dimensions.paddingDefault))
                            }
                            item {
                                Divider(color = RadixTheme.colors.gray5)
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.version_and_build,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE),
                        style = RadixTheme.typography.body2Link,
                        color = RadixTheme.colors.gray2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultSettingsItem(
    settingsItem: SettingSectionItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        settingsItem.getIcon()?.let {
            Icon(painter = painterResource(id = it), contentDescription = null)
        }
        settingsItem.descriptionRes()?.let { desc ->
            Text(
                text = stringResource(id = desc),
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.gray1
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.gray1)
    }
}

@Composable
private fun ConnectionSettingItem(
    onSettingClick: (SettingSectionItem) -> Unit,
    settingsItem: SettingSectionItem,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)) {
        Image(painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_connector),
            contentDescription = null)
        Text(
            text = stringResource(R.string.link_your_wallet),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.scan_the_qr_code),
            style = RadixTheme.typography.body2Link,
            color = RadixTheme.colors.gray2,
            textAlign = TextAlign.Center
        )
        RadixSecondaryButton(text = stringResource(R.string.link_to_connector), onClick = {
            onSettingClick(settingsItem)
        }, contentColor = RadixTheme.colors.gray1, icon = {
            Icon(painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner),
                contentDescription = null)
        })
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenWithoutActiveConnectionPreview() {
    RadixWalletTheme {
        SettingsContent(
            onBackClick = {},
            appSettings = persistentListOf(
                SettingSectionItem.Connection,
                SettingSectionItem.LinkedConnector,
                SettingSectionItem.Gateway,
                SettingSectionItem.DeleteAll
            ),
            onSettingClick = {}
        )
    }
}
