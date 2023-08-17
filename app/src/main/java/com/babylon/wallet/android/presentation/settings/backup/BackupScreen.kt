package com.babylon.wallet.android.presentation.settings.backup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.NotBackedUpWarning
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
import com.babylon.wallet.android.utils.formattedSpans
import rdx.works.profile.data.model.BackupState

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier,
    onSystemBackupSettingsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackupScreenContent(
        modifier = modifier,
        state = state,
        onBackupCheckChanged = { isChecked ->
            viewModel.onBackupSettingChanged(isChecked)
        },
        onSystemBackupSettingsClick = onSystemBackupSettingsClick,
        onBackClick = onBackClick
    )
}

@Composable
private fun BackupScreenContent(
    modifier: Modifier = Modifier,
    state: BackupViewModel.State,
    onBackupCheckChanged: (Boolean) -> Unit,
    onSystemBackupSettingsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.settings_backups),
                onBackClick = onBackClick
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.profileBackup_headerTitle)
                    .formattedSpans(boldStyle = SpanStyle(fontWeight = FontWeight.Bold)),
                color = RadixTheme.colors.gray2
            )

            Text(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.profileBackup_automaticBackups_title)
                    .formattedSpans(boldStyle = SpanStyle(fontWeight = FontWeight.Bold)),
                color = RadixTheme.colors.gray2
            )

            Surface(
                color = RadixTheme.colors.defaultBackground,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingLarge
                        ),
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                ) {
                    SwitchSettingsItem(
                        titleRes = if (state.isBackupEnabled) {
                            R.string.androidProfileBackup_automaticBackups_disable
                        } else {
                            R.string.androidProfileBackup_automaticBackups_enable
                        },
                        subtitleRes = R.string.androidProfileBackup_automaticBackups_subtitle,
                        checked = state.isBackupEnabled,
                        icon = {
                            Icon(
                                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_backup),
                                tint = RadixTheme.colors.gray1,
                                contentDescription = null
                            )
                        },
                        onCheckedChange = onBackupCheckChanged
                    )

                    NotBackedUpWarning(
                        backupState = state.backupState,
                        horizontalSpacing = RadixTheme.dimensions.paddingMedium
                    )

                    if (isBackupScreenNavigationSupported()) {
                        RadixPrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.androidProfileBackup_openSystemBackupSettings),
                            onClick = onSystemBackupSettingsClick
                        )
                    }
                }
            }


        }
    }
}

@Preview
@Composable
private fun BackupScreenPreview() {
    RadixWalletTheme {
        BackupScreenContent(
            state = BackupViewModel.State(backupState = BackupState.Closed),
            onBackupCheckChanged = {},
            onSystemBackupSettingsClick = {},
            onBackClick = {}
        )
    }
}
