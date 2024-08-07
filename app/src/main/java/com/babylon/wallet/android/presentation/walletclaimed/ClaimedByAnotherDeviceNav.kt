package com.babylon.wallet.android.presentation.walletclaimed

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.cloudbackup.model.BackupServiceException
import rdx.works.profile.domain.backup.CloudBackupFileEntity

private const val ARG_CLAIMED_ENTITY = "arg_claimed_entity"
private const val ROUTE = "route_claimed_by_another_device/{$ARG_CLAIMED_ENTITY}"

internal class ClaimedByAnotherDeviceArgs(
    val claimedEntity: CloudBackupFileEntity
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        claimedEntity = Json.decodeFromString(requireNotNull(savedStateHandle.get<String>(ARG_CLAIMED_ENTITY)))
    )
}

fun NavController.navigateToClaimedByAnotherDevice(error: BackupServiceException.ClaimedByAnotherDevice) {
    val entityEncoded = Json.encodeToString(error.fileEntity)
    navigate(
        route = "route_claimed_by_another_device/$entityEncoded"
    )
}

fun NavGraphBuilder.claimedByAnotherDevice(
    onNavigateToOnboarding: () -> Unit,
    onReclaimedBack: () -> Unit
) {
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(ARG_CLAIMED_ENTITY) {
                type = NavType.StringType
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        ClaimedByAnotherDeviceScreen(
            viewModel = hiltViewModel(),
            onNavigateToOnboarding = onNavigateToOnboarding,
            onReclaimedBack = onReclaimedBack
        )
    }
}
