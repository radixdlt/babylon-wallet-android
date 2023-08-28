package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.google.accompanist.navigation.animation.composable
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.domain.backup.BackupType

private const val ARGS_RESTORE_MNEMONICS = "restoreMnemonicsArgs"
private const val ROUTE = "restore_mnemonics?restoreMnemonicsArgs={$ARGS_RESTORE_MNEMONICS}"

fun NavController.restoreMnemonics(
    args: RestoreMnemonicsArgs
) {
    navigate(route = "restore_mnemonics?restoreMnemonicsArgs={${Json.encodeToString(args)}}")
}

@Serializable
sealed interface RestoreMnemonicsArgs {
    @Serializable
    data class RestoreProfile(val backupType: BackupType) : RestoreMnemonicsArgs
    @Serializable
    data class RestoreSpecificMnemonic(
        val factorSourceId: FactorSource.HexCoded32Bytes,
        val isMandatory: Boolean = false
    ) : RestoreMnemonicsArgs

    companion object {
        fun from(savedStateHandle: SavedStateHandle): RestoreMnemonicsArgs {
            val serialised: String = requireNotNull(savedStateHandle[ARGS_RESTORE_MNEMONICS])

            return Json.decodeFromString(serialised)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.restoreMnemonicsScreen(
    onCloseApp: () -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(
                name = ARGS_RESTORE_MNEMONICS,
            ) {
                nullable = false
                type = NavType.StringType
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        RestoreMnemonicsScreen(
            viewModel = hiltViewModel(),
            onCloseApp = onCloseApp,
            onDismiss = onDismiss
        )
    }
}
