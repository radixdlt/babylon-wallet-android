package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.domain.backup.BackupType

private const val ARGS_BACKUP_TYPE = "backup_type"
private const val ROUTE = "restore_mnemonics?$ARGS_BACKUP_TYPE={$ARGS_BACKUP_TYPE}"

fun NavController.restoreMnemonics(
    args: RestoreMnemonicsArgs
) {
    val backupType = Json.encodeToString(args.backupType)
    navigate(route = "restore_mnemonics?$ARGS_BACKUP_TYPE=$backupType")
}

@Serializable
data class RestoreMnemonicsArgs(
    val backupType: BackupType? = null
) {
    companion object {
        fun from(savedStateHandle: SavedStateHandle): RestoreMnemonicsArgs {
            val backupType: BackupType? = savedStateHandle.get<String>(ARGS_BACKUP_TYPE)?.let {
                Json.decodeFromString(it)
            }
            return RestoreMnemonicsArgs(backupType)
        }
    }
}

fun NavGraphBuilder.restoreMnemonicsScreen(
    onCloseApp: () -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(
                name = ARGS_BACKUP_TYPE,
            ) {
                nullable = true
                type = NavType.StringType
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        RestoreMnemonicsScreen(
            viewModel = hiltViewModel(),
            onCloseApp = onCloseApp,
            onDismiss = onDismiss
        )
    }
}
