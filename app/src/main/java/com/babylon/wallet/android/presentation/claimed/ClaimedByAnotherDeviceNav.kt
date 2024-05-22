package com.babylon.wallet.android.presentation.claimed

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.radixdlt.sargon.Timestamp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.cloudbackup.BackupServiceException
import rdx.works.profile.domain.backup.CloudBackupFileEntity
import java.time.format.DateTimeFormatter

private const val ARG_CLAIMED_ENTITY = "claimed_entity"
private const val ARG_MODIFIED_TIME = "modified_time"
private const val ROUTE = "route_claimed_by_another_device/{$ARG_CLAIMED_ENTITY}?$ARG_MODIFIED_TIME={$ARG_MODIFIED_TIME}"

internal class ClaimedByAnotherDeviceArgs(
    val claimedEntity: CloudBackupFileEntity,
    val modifiedTime: Timestamp
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        claimedEntity = Json.decodeFromString(requireNotNull(savedStateHandle.get<String>(ARG_CLAIMED_ENTITY))),
        modifiedTime = Timestamp.parse(requireNotNull(savedStateHandle.get<String>(ARG_MODIFIED_TIME)), DateTimeFormatter.ISO_DATE_TIME)
    )
}

fun NavController.navigateToClaimedByAnotherDevice(error: BackupServiceException.ClaimedByAnotherDevice) {
    val entityEncoded = Json.encodeToString(error.fileEntity)
    val timeEncoded = error.claimedProfileModifiedTime.format(DateTimeFormatter.ISO_DATE_TIME)
    navigate(
        route = "route_claimed_by_another_device/$entityEncoded?$ARG_MODIFIED_TIME=$timeEncoded"
    )
}

fun NavGraphBuilder.claimedByAnotherDevice(
    onNavigateToOnboarding: () -> Unit,
    onDismiss: () -> Unit
) {
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(ARG_CLAIMED_ENTITY) {
                type = NavType.StringType
            },
            navArgument(ARG_MODIFIED_TIME) {
                type = NavType.StringType
            }
        )
    ) {
        ClaimedByAnotherDeviceScreen(
            viewModel = hiltViewModel(),
            onNavigateToOnboarding = onNavigateToOnboarding,
            onDismiss = onDismiss
        )
    }
}
