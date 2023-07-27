package com.babylon.wallet.android.presentation.settings.backup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.NotBackedUpWarning
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
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
        modifier = modifier
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
            title = stringResource(R.string.settings_backups),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Divider(color = RadixTheme.colors.gray5)

        SwitchSettingsItem(
            modifier = Modifier.padding(all = RadixTheme.dimensions.paddingDefault),
            titleRes = R.string.androidProfileBackup_backupWalletData_title,
            subtitleRes = R.string.androidProfileBackup_backupWalletData_message,
            checked = state.backupState is BackupState.Open,
            onCheckedChange = onBackupCheckChanged
        )

        Divider(color = RadixTheme.colors.gray5)

        NotBackedUpWarning(backupState = state.backupState, modifier = Modifier.padding(all = RadixTheme.dimensions.paddingDefault))

        if (isBackupScreenNavigationSupported()) {
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.androidProfileBackup_openSystemBackupSettings),
                onClick = onSystemBackupSettingsClick
            )
        }
    }
}
