package com.babylon.wallet.android.presentation.settings.securitycenter.backup

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ResolveInfoFlags
import android.os.Build
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

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

fun NavController.backupScreen() {
    navigate("settings_backup") {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.backupScreen(
    onProfileDeleted: () -> Unit,
    onClose: () -> Unit
) {
    composable(
        route = "settings_backup",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        BackupScreen(
            viewModel = hiltViewModel(),
            onProfileDeleted = onProfileDeleted,
            onClose = onClose
        )
    }
}
