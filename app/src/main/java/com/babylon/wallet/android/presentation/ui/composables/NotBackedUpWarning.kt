package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import rdx.works.profile.data.model.BackupState

@Composable
fun NotBackedUpWarning(backupState: BackupState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (backupState.isWarningVisible) {
            Icon(
                painter = painterResource(id = R.drawable.ic_warning_error),
                contentDescription = null,
                tint = RadixTheme.colors.orange1
            )
        }
        Text(
            text = backupMessage(state = backupState),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.orange1
        )
    }
}

@Composable
private fun backupMessage(state: BackupState) = when (state) {
    is BackupState.Closed -> stringResource(id = com.babylon.wallet.android.R.string.androidProfileBackup_disabledText)
    is BackupState.Open -> {
        val lastBackupRelativeTime = remember(state) { state.lastBackupTimeRelative }

        if (lastBackupRelativeTime != null) {
            stringResource(id = com.babylon.wallet.android.R.string.androidProfileBackup_lastBackedUp, lastBackupRelativeTime)
        } else {
            stringResource(id = com.babylon.wallet.android.R.string.androidProfileBackup_noLastBackUp)
        }
    }
}
