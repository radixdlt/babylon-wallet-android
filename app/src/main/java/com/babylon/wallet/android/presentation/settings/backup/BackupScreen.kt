package com.babylon.wallet.android.presentation.settings.backup

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.Orange1
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
import rdx.works.profile.data.model.BackupState

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onSystemBackupSettingsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackupScreenContent(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
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
    Column(
        modifier = modifier,
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.backups),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Divider(color = RadixTheme.colors.gray5)

        SwitchSettingsItem(
            modifier = Modifier.padding(all = RadixTheme.dimensions.paddingDefault),
            titleRes = R.string.backup_wallet_data,
            subtitleRes = R.string.backup_wallet_data_message,
            checked = state.backupState is BackupState.Open,
            onCheckedChange = onBackupCheckChanged
        )

        Divider(color = RadixTheme.colors.gray5)

        Row(
            modifier = Modifier.padding(all = RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val subtitle = when (val backupState = state.backupState) {
                is BackupState.Closed -> {
                    "Back up is turned off"
                }
                is BackupState.Open -> {
                    val lastBackupRelativeTime = remember(backupState.lastBackup) {
                        backupState.lastBackup?.toEpochMilli()?.let { timeInMilliseconds ->
                            DateUtils.getRelativeTimeSpanString(timeInMilliseconds)
                        }
                    }

                    if (lastBackupRelativeTime != null) {
                        "Last Backed up: $lastBackupRelativeTime"
                    } else {
                        "Not backed up yet"
                    }
                }
            }

            Text(
                text = subtitle,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.defaultText
            )

            if (state.backupState.isWarningVisible) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error),
                    contentDescription = null,
                    tint = Orange1
                )
            }
        }

        RadixPrimaryButton(
            modifier = Modifier.fillMaxWidth().padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = "System Backup Settings",
            onClick = onSystemBackupSettingsClick
        )
    }
}
