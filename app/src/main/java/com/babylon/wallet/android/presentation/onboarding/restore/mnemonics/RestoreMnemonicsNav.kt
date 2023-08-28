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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.domain.backup.BackupType

private const val ARG_FACTOR_SOURCE_ID = "factorSourceId"
private const val ARG_BACKUP_TYPE = "backupType"
private const val ROUTE = "restore_mnemonics?factorSourceId={$ARG_FACTOR_SOURCE_ID}&backupType={$ARG_BACKUP_TYPE}"

fun NavController.restoreMnemonics(
    deviceFactorSourceId: FactorSource.FactorSourceID.FromHash? = null,
    backupType: BackupType? = null
) {
    navigate(
        route = if (deviceFactorSourceId != null) {
            "restore_mnemonics?factorSourceId=${deviceFactorSourceId.body.value}"
        } else if (backupType != null) {
            "restore_mnemonics?backupType=${Json.encodeToString(backupType)}"
        } else {
            error("Need to specify the type of backup which we are restoring from")
        }
    )
}

sealed class RestoreMnemonicsArgs {
    data class RestoreProfile(val backupType: BackupType) : RestoreMnemonicsArgs()
    data class RestoreSpecificMnemonic(val factorSourceIdHex: String) : RestoreMnemonicsArgs()

    companion object {
        fun from(savedStateHandle: SavedStateHandle): RestoreMnemonicsArgs {
            val factorSourceIdHex: String? = savedStateHandle[ARG_FACTOR_SOURCE_ID]
            return if (factorSourceIdHex != null) {
                RestoreSpecificMnemonic(factorSourceIdHex)
            } else {
                val type: String = requireNotNull(savedStateHandle[ARG_BACKUP_TYPE])
                RestoreProfile(Json.decodeFromString(type))
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.restoreMnemonicsScreen(
    onFinish: (Boolean) -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(
                name = ARG_FACTOR_SOURCE_ID,
            ) {
                nullable = true
                type = NavType.StringType
            },
            navArgument(
                name = ARG_BACKUP_TYPE,
            ) {
                nullable = true
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
            onFinish = onFinish
        )
    }
}
