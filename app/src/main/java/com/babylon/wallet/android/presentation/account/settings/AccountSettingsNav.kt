package com.babylon.wallet.android.presentation.account.settings

import androidx.annotation.VisibleForTesting
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
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

@VisibleForTesting
internal const val ARG_ACCOUNT_SETTINGS_ADDRESS = "arg_account_settings_address"

internal class AccountSettingsArgs(
    val address: AccountAddress
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        AccountAddress.init(checkNotNull(savedStateHandle[ARG_ACCOUNT_SETTINGS_ADDRESS]) as String)
    )
}

fun NavController.accountSettings(
    address: AccountAddress
) {
    navigate("account_settings_route/${address.string}") {
        launchSingleTop = true
    }
}

@Suppress("LongParameterList")
fun NavGraphBuilder.accountSettings(
    onBackClick: () -> Unit,
    onAccountSettingItemClick: (AccountSettingItem, address: AccountAddress) -> Unit,
    onHideAccountClick: () -> Unit,
    onDeleteAccountClick: (AccountAddress) -> Unit,
    onFactorSourceCardClick: (FactorSourceId) -> Unit
) {
    composable(
        route = "account_settings_route/{$ARG_ACCOUNT_SETTINGS_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_ACCOUNT_SETTINGS_ADDRESS) { type = NavType.StringType }
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
        AccountSettingsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onSettingItemClick = onAccountSettingItemClick,
            onHideAccountClick = onHideAccountClick,
            onDeleteAccountClick = onDeleteAccountClick,
            onFactorSourceCardClick = onFactorSourceCardClick
        )
    }
}
