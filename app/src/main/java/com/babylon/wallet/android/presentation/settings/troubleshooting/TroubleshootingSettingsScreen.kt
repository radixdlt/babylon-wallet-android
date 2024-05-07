package com.babylon.wallet.android.presentation.settings.troubleshooting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem.Troubleshooting
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.utils.openEmail
import com.babylon.wallet.android.utils.openUrl
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
fun TroubleshootingSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: TroubleshootingSettingsViewModel = hiltViewModel(),
    onSettingItemClick: (Troubleshooting) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TroubleshootingSettingsContent(
        modifier = modifier.fillMaxSize(),
        settings = state.settings,
        onSettingItemClick = onSettingItemClick,
        onBackClick = onBackClick,
    )
}

@Composable
private fun TroubleshootingSettingsContent(
    modifier: Modifier = Modifier,
    settings: ImmutableSet<TroubleshootingUiItem>,
    onSettingItemClick: (Troubleshooting) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.troubleshooting_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.gray5)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                settings.forEach { troubleshootingItem ->
                    item {
                        when (troubleshootingItem) {
                            TroubleshootingUiItem.RecoverySection -> {
                                Text(
                                    modifier = Modifier.padding(all = RadixTheme.dimensions.paddingDefault),
                                    text = stringResource(id = R.string.troubleshooting_accountRecovery),
                                    style = RadixTheme.typography.body1Link,
                                    color = RadixTheme.colors.gray2
                                )
                            }

                            TroubleshootingUiItem.SupportSection -> {
                                Text(
                                    modifier = Modifier.padding(all = RadixTheme.dimensions.paddingDefault),
                                    text = stringResource(id = R.string.troubleshooting_supportAndCommunity),
                                    style = RadixTheme.typography.body1Link,
                                    color = RadixTheme.colors.gray2
                                )
                            }

                            is TroubleshootingUiItem.Setting -> {
                                val context = LocalContext.current
                                val item = troubleshootingItem.item
                                DefaultSettingsItem(
                                    title = stringResource(id = item.descriptionRes()),
                                    leadingIcon = item.getIcon(),
                                    subtitle = stringResource(id = item.subtitleRes()),
                                    onClick = {
                                        when (item) {
                                            Troubleshooting.ContactSupport -> context.openEmail()
                                            Troubleshooting.Discord -> context.openUrl("http://discord.gg/radixdlt")
                                            else -> {
                                                onSettingItemClick(item)
                                            }
                                        }
                                    },
                                    trailingIcon = when (item) {
                                        Troubleshooting.ContactSupport,
                                        Troubleshooting.Discord -> {
                                            {
                                                Icon(
                                                    painter = painterResource(id = DSR.ic_link_out),
                                                    contentDescription = null,
                                                    tint = RadixTheme.colors.gray1
                                                )
                                            }
                                        }

                                        else -> null
                                    }
                                )
                                HorizontalDivider(color = RadixTheme.colors.gray5)
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
fun TroubleshootingSettingsContentPreview() {
    RadixWalletTheme {
        TroubleshootingSettingsContent(
            modifier = Modifier,
            settings = persistentSetOf(),
            onSettingItemClick = {},
            onBackClick = {}
        )
    }
}
