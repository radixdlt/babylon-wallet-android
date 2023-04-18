package com.babylon.wallet.android.presentation.settings.backup

import android.content.ComponentName
import android.content.Intent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import timber.log.Timber

fun NavController.systemBackupSettingsScreen() {
    try {
        val intent = Intent().apply {
            component = ComponentName(
                "com.google.android.gms",
                "com.google.android.gms.backup.component.BackupSettingsActivity"
            )
        }
        context.startActivity(intent)
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
    onBackClick: () -> Unit
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
            onBackClick = onBackClick
        )
    }
}
