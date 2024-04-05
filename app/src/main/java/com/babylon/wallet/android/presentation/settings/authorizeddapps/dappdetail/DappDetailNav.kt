package com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.settings.personas.personaedit.ROUTE_EDIT_PERSONA
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.resources.Resource

@VisibleForTesting
internal const val ARG_DAPP_ADDRESS = "dapp_definition_address"

const val ROUTE_DAPP_DETAIL = "settings_dapp_detail/{$ARG_DAPP_ADDRESS}"

internal class DappDetailScreenArgs(val dappDefinitionAddress: AccountAddress) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        AccountAddress.init(checkNotNull(savedStateHandle[ARG_DAPP_ADDRESS]))
    )
}

fun NavController.dAppDetailScreen(dappDefinitionAddress: AccountAddress) {
    navigate("settings_dapp_detail/${dappDefinitionAddress.string}")
}

fun NavGraphBuilder.dAppDetailScreen(
    onBackClick: () -> Unit,
    onEditPersona: (String, RequiredPersonaFields?) -> Unit,
    onFungibleClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit
) {
    composable(
        route = ROUTE_DAPP_DETAIL,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            when (targetState.destination.route) {
                ROUTE_EDIT_PERSONA -> null
                else -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            }
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        },
        arguments = listOf(
            navArgument(ARG_DAPP_ADDRESS) {
                type = NavType.StringType
            }
        )
    ) {
        DappDetailScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onEditPersona = onEditPersona,
            onFungibleClick = onFungibleClick,
            onNonFungibleClick = onNonFungibleClick
        )
    }
}
