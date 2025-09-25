package com.babylon.wallet.android.presentation.account

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
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.resources.Resource

private const val ROUTE_ACCOUNT = "account_route"
private const val ARG_ACCOUNT_ADDRESS = "arg_account_address"

class AccountArgs private constructor(
    val accountAddress: AccountAddress
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        accountAddress = AccountAddress.init(validatingAddress = requireNotNull(savedStateHandle.get<String>(ARG_ACCOUNT_ADDRESS)))
    )
}

fun NavController.account(accountAddress: AccountAddress) {
    navigate(route = "$ROUTE_ACCOUNT/${accountAddress.string}")
}

@Suppress("LongParameterList")
fun NavGraphBuilder.account(
    onAccountPreferenceClick: (AccountAddress) -> Unit,
    onBackClick: () -> Unit,
    onFungibleResourceClick: (Resource.FungibleResource, Account) -> Unit,
    onNonFungibleResourceClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Account) -> Unit,
    onTransferClick: (AccountAddress) -> Unit,
    onHistoryClick: (AccountAddress) -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    composable(
        route = "$ROUTE_ACCOUNT/{$ARG_ACCOUNT_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_ACCOUNT_ADDRESS) {
                type = NavType.StringType
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
        AccountScreen(
            viewModel = hiltViewModel(),
            onAccountPreferenceClick = onAccountPreferenceClick,
            onBackClick = onBackClick,
            onNavigateToSecurityCenter = onNavigateToSecurityCenter,
            onFungibleResourceClick = onFungibleResourceClick,
            onNonFungibleResourceClick = onNonFungibleResourceClick,
            onTransferClick = onTransferClick,
            onHistoryClick = onHistoryClick,
            onInfoClick = onInfoClick
        )
    }
}
