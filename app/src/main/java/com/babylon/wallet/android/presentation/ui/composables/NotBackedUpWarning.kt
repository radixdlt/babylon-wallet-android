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
import androidx.compose.ui.unit.Dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import rdx.works.core.domain.cloudbackup.CloudBackupState

@Composable
fun NotBackedUpWarning(
    modifier: Modifier = Modifier,
    cloudBackupState: CloudBackupState,
    horizontalSpacing: Dp = RadixTheme.dimensions.paddingSmall
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (cloudBackupState.isDisabled) {
            Icon(
                painter = painterResource(id = R.drawable.ic_warning_error),
                contentDescription = null,
                tint = RadixTheme.colors.orange1
            )
        }
        Text(
            text = cloudBackupState.title(),
            style = RadixTheme.typography.body2Regular,
            color = cloudBackupState.color()
        )
    }
}

// @Composable
// private fun BackupState.title() = when (this) {
//    is BackupState.Closed -> stringResource(id = com.babylon.wallet.android.R.string.androidProfileBackup_disabledText)
//    is BackupState.Open -> {
//        val lastBackupRelativeTime = remember(this) { lastBackupTimeRelative }
//
//        if (lastBackupRelativeTime != null) {
//            stringResource(id = com.babylon.wallet.android.R.string.androidProfileBackup_lastBackedUp, lastBackupRelativeTime)
//        } else {
//            stringResource(id = com.babylon.wallet.android.R.string.androidProfileBackup_noLastBackUp)
//        }
//    }
// }

@Composable
fun CloudBackupState.title() = when (this) {
    is CloudBackupState.Disabled -> {
        val lastBackupRelativeTime = remember(this) { lastBackup }
        stringResource(id = com.babylon.wallet.android.R.string.androidProfileBackup_lastBackedUp, lastBackupRelativeTime)
    }
    is CloudBackupState.Enabled -> {
        stringResource(id = com.babylon.wallet.android.R.string.empty)
    }
}

@Composable
private fun CloudBackupState.color() = when (this) {
    is CloudBackupState.Disabled -> RadixTheme.colors.orange1
    is CloudBackupState.Enabled -> {
        RadixTheme.colors.gray1
    }
}

// @Composable
// private fun BackupState.color() = when (this) {
//    is BackupState.Closed -> RadixTheme.colors.orange1
//    is BackupState.Open -> {
//        val lastBackupRelativeTime = remember(this) { lastBackupTimeRelative }
//
//        if (lastBackupRelativeTime != null) {
//            RadixTheme.colors.gray1
//        } else {
//            RadixTheme.colors.orange1
//        }
//    }
// }
