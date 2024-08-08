package com.babylon.wallet.android.presentation.account.createaccount.confirmation

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
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

@VisibleForTesting
internal const val ARG_ACCOUNT_ID = "arg_account_id"

@VisibleForTesting
internal const val ARG_REQUEST_SOURCE = "arg_request_source"

private const val ROUTE = "account_completion_route/{$ARG_REQUEST_SOURCE}/{$ARG_ACCOUNT_ID}"

internal class CreateAccountConfirmationArgs(val accountId: AccountAddress, val requestSource: CreateAccountRequestSource) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        AccountAddress.init(checkNotNull(savedStateHandle[ARG_ACCOUNT_ID]) as String),
        checkNotNull(savedStateHandle[ARG_REQUEST_SOURCE]) as CreateAccountRequestSource
    )
}

fun NavController.createAccountConfirmationScreen(
    accountId: AccountAddress,
    requestSource: CreateAccountRequestSource
) {
    navigate("account_completion_route/$requestSource/${accountId.string}")
}

enum class CreateAccountRequestSource {
    AccountsList, ChooseAccount, FirstTimeWithCloudBackupDisabled, FirstTimeWithCloudBackupEnabled, Gateways;

    fun isFirstTime() = (this == FirstTimeWithCloudBackupDisabled || this == FirstTimeWithCloudBackupEnabled)
}

fun NavGraphBuilder.createAccountConfirmationScreen(
    onNavigateToWallet: () -> Unit,
    onFinishAccountCreation: () -> Unit,
) {
    markAsHighPriority(route = ROUTE)
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(ARG_REQUEST_SOURCE) {
                type = NavType.EnumType(
                    CreateAccountRequestSource::class.java
                )
            },
            navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
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
        CreateAccountConfirmationScreen(
            viewModel = hiltViewModel(),
            navigateToWallet = onNavigateToWallet,
            finishAccountCreation = onFinishAccountCreation
        )
    }
}
