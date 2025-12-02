package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.applyShieldNavGraph
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.factorsourcedetails.factorSourceDetails
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.ROUTE_CREATE_SECURITY_SHIELD_GRAPH
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess.regularAccess
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

private const val ARG_SECURITY_STRUCTURE_ID = "arg_security_structure_id"
private const val ARG_ACCOUNT_ADDRESS = "arg_account_address"
private const val DESTINATION_SECURITY_SHIELD_DETAILS_SCREEN = "security_shield_details_screen"
private const val ROUTE_SECURITY_SHIELD_DETAILS_SCREEN = DESTINATION_SECURITY_SHIELD_DETAILS_SCREEN +
    "?$ARG_SECURITY_STRUCTURE_ID={$ARG_SECURITY_STRUCTURE_ID}" +
    "&$ARG_ACCOUNT_ADDRESS={$ARG_ACCOUNT_ADDRESS}"

internal class SecurityShieldDetailsArgs(
    val input: Input
) {

    sealed interface Input {

        data class Id(
            val value: SecurityStructureId
        ) : Input

        data class Address(
            val value: AddressOfAccountOrPersona
        ) : Input
    }

    constructor(savedStateHandle: SavedStateHandle) : this(
        input = savedStateHandle.get<String>(ARG_SECURITY_STRUCTURE_ID).let { id ->
            if (id == null) {
                val arg = requireNotNull(savedStateHandle.get<String>(ARG_ACCOUNT_ADDRESS))
                Input.Address(
                    value = AddressOfAccountOrPersona.init(arg)
                )
            } else {
                Input.Id(
                    value = SecurityStructureId.fromString(id)
                )
            }
        }
    )
}

fun NavController.securityShieldDetails(
    securityStructureId: SecurityStructureId
) {
    navigate("$DESTINATION_SECURITY_SHIELD_DETAILS_SCREEN?$ARG_SECURITY_STRUCTURE_ID=$securityStructureId")
}

fun NavController.securityShieldDetails(
    addressOfAccountOrPersona: AddressOfAccountOrPersona
) {
    navigate("$DESTINATION_SECURITY_SHIELD_DETAILS_SCREEN?$ARG_ACCOUNT_ADDRESS=${addressOfAccountOrPersona.string}")
}

fun NavGraphBuilder.securityShieldDetails(navController: NavController) {
    composable(
        route = ROUTE_SECURITY_SHIELD_DETAILS_SCREEN,
        arguments = listOf(
            navArgument(ARG_SECURITY_STRUCTURE_ID) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ARG_ACCOUNT_ADDRESS) {
                type = NavType.StringType
                nullable = true
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
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        SecurityShieldDetailsScreen(
            viewModel = hiltViewModel(),
            onBackClick = { navController.navigateUp() },
            onFactorClick = { id -> navController.factorSourceDetails(id) },
            onApplyShieldClick = { id ->
                navController.applyShieldNavGraph(id) {
                    popUpTo(ROUTE_CREATE_SECURITY_SHIELD_GRAPH) { inclusive = false }
                }
            },
            onInfoClick = { navController.infoDialog(it) },
            onEditShield = { navController.regularAccess() }
        )
    }
}
