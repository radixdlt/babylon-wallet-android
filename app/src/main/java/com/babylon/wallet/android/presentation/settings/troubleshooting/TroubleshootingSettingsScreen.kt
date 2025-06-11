package com.babylon.wallet.android.presentation.settings.troubleshooting

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.babylon.wallet.android.presentation.discover.common.models.SocialLinkType
import com.babylon.wallet.android.presentation.settings.SettingsItem.Troubleshooting
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType = "text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.onExportLogsToFile(file = uri)
        }
    }

    TroubleshootingSettingsContent(
        modifier = modifier.fillMaxSize(),
        settings = state.settings,
        onSettingItemClick = { item ->
            when (item) {
                Troubleshooting.ExportLogs -> filePickerLauncher.launch("radix-wallet-logs.txt")
                else -> onSettingItemClick(item)
            }
        },
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
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.troubleshooting_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                settings.forEach { troubleshootingItem ->
                    item {
                        when (troubleshootingItem) {
                            TroubleshootingUiItem.RecoverySection -> {
                                SectionHeader(title = stringResource(id = R.string.troubleshooting_accountRecovery))
                            }

                            TroubleshootingUiItem.SupportSection -> {
                                SectionHeader(title = stringResource(id = R.string.troubleshooting_supportAndCommunity))
                            }

                            TroubleshootingUiItem.ResetSection -> {
                                SectionHeader(title = stringResource(id = R.string.troubleshooting_resetAccount))
                            }

                            is TroubleshootingUiItem.Setting -> {
                                val context = LocalContext.current
                                val item = troubleshootingItem.item
                                DefaultSettingsItem(
                                    title = stringResource(id = item.titleRes()),
                                    leadingIconRes = item.getIcon(),
                                    subtitle = stringResource(id = item.subtitleRes()),
                                    onClick = {
                                        when (item) {
                                            is Troubleshooting.ContactSupport -> {
                                                context.openEmail(item.supportAddress, item.subject, item.body)
                                            }
                                            Troubleshooting.Discord -> context.openUrl(SocialLinkType.Discord.url)
                                            else -> {
                                                onSettingItemClick(item)
                                            }
                                        }
                                    },
                                    trailingIcon = when (item) {
                                        is Troubleshooting.ContactSupport,
                                        is Troubleshooting.ExportLogs,
                                        Troubleshooting.Discord -> {
                                            {
                                                Icon(
                                                    painter = painterResource(id = DSR.ic_link_out),
                                                    contentDescription = null,
                                                    tint = RadixTheme.colors.icon
                                                )
                                            }
                                        }

                                        else -> {
                                            {
                                                Icon(
                                                    painter = painterResource(id = DSR.ic_chevron_right),
                                                    contentDescription = null,
                                                    tint = RadixTheme.colors.icon
                                                )
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider(color = RadixTheme.colors.divider)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(modifier: Modifier = Modifier, title: String) {
    Text(
        modifier = modifier.padding(all = RadixTheme.dimensions.paddingDefault),
        text = title,
        style = RadixTheme.typography.body1Link,
        color = RadixTheme.colors.textSecondary
    )
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
