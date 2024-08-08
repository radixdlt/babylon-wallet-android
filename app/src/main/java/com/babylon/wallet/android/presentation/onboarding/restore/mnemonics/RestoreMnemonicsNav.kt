package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import android.os.Build
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.babylon.wallet.android.presentation.settings.personas.createpersona.ARG_REQUEST_SOURCE
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.domain.backup.BackupType

private const val ARGS_REQUEST_SOURCE = "arg_request_source"
private const val ARGS_BACKUP_TYPE = "backup_type"
private const val ROUTE = "restore_mnemonics?$ARGS_BACKUP_TYPE={$ARGS_BACKUP_TYPE}&$ARGS_REQUEST_SOURCE={$ARGS_REQUEST_SOURCE}"

enum class RestoreMnemonicsRequestSource {
    Onboarding, Settings
}

fun NavController.restoreMnemonics(
    args: RestoreMnemonicsArgs
) {
    val backupType = Json.encodeToString(args.backupType)
    navigate(route = "restore_mnemonics?$ARGS_BACKUP_TYPE=$backupType&$ARGS_REQUEST_SOURCE=${args.requestSource}")
}

@Serializable
data class RestoreMnemonicsArgs(
    val backupType: BackupType? = null,
    val requestSource: RestoreMnemonicsRequestSource
) {
    companion object {
        fun from(savedStateHandle: SavedStateHandle): RestoreMnemonicsArgs {
            val backupType: BackupType? = savedStateHandle.get<String>(ARGS_BACKUP_TYPE)?.let {
                Json.decodeFromString(it)
            }
            val requestSource = checkNotNull(savedStateHandle[ARGS_REQUEST_SOURCE]) as RestoreMnemonicsRequestSource
            return RestoreMnemonicsArgs(backupType, requestSource)
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
            },
            navArgument(ARGS_REQUEST_SOURCE) {
                type = NavType.EnumType(RestoreMnemonicsRequestSource::class.java)
            }
        ),
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
            slideOutOfContainer(
                when (initialState.getRequestSource()) {
                    RestoreMnemonicsRequestSource.Onboarding -> AnimatedContentTransitionScope.SlideDirection.Left
                    RestoreMnemonicsRequestSource.Settings -> AnimatedContentTransitionScope.SlideDirection.Right
                }
            )
        }
    ) {
        RestoreMnemonicsScreen(
            viewModel = hiltViewModel(),
            onCloseApp = onCloseApp,
            onDismiss = onDismiss
        )
    }
}

private fun NavBackStackEntry.getRequestSource(): RestoreMnemonicsRequestSource {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        checkNotNull(arguments?.getSerializable(ARG_REQUEST_SOURCE, RestoreMnemonicsRequestSource::class.java))
    } else {
        arguments?.getSerializable(ARG_REQUEST_SOURCE) as RestoreMnemonicsRequestSource
    }
}
