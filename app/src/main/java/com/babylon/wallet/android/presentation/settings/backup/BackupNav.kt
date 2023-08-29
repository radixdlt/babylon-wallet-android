package com.babylon.wallet.android.presentation.settings.backup

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ResolveInfoFlags
import android.os.Build
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import timber.log.Timber

private val backupSettingsScreenIntent: Intent
    get() = Intent().apply {
        component = ComponentName(
            "com.google.android.gms",
            "com.google.android.gms.backup.component.BackupSettingsActivity"
        )
    }

@SuppressLint("QueryPermissionsNeeded")
@Composable
fun isBackupScreenNavigationSupported(): Boolean {
    val context = LocalContext.current
    return remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(backupSettingsScreenIntent, ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            context.packageManager.queryIntentActivities(backupSettingsScreenIntent, PackageManager.MATCH_ALL)
        }.size > 0
    }
}

fun NavController.systemBackupSettingsScreen() {
    try {
        context.startActivity(backupSettingsScreenIntent)
    } catch (e: Exception) {
        Timber.w(e)
    }
}

fun NavController.backupScreen() {
    navigate("settings_backup")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.backupScreen(
    onSystemBackupSettingsClick: () -> Unit,
    onProfileDeleted: () -> Unit,
    onClose: () -> Unit
) {
    composable(
        route = "settings_backup",
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        BackupScreen(
            viewModel = hiltViewModel(),
            onSystemBackupSettingsClick = onSystemBackupSettingsClick,
            onProfileDeleted = onProfileDeleted,
            onClose = onClose
        )
    }
}
