package com.babylon.wallet.android.presentation.account.createaccount.withledger

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.babylon.wallet.android.utils.Constants
import rdx.works.profile.data.model.factorsources.FactorSource

@VisibleForTesting
const val ARG_NETWORK_ID = "arg_network_id"

@VisibleForTesting
const val ARG_SELECTION_PURPOSE = "arg_selection_purpose"

private const val ROUTE_CHOOSE_LEDGER = "route_choose_ledger?$ARG_NETWORK_ID" +
    "={$ARG_NETWORK_ID}&$ARG_SELECTION_PURPOSE={$ARG_SELECTION_PURPOSE}"

internal class ChooserLedgerArgs(
    val networkId: Int,
    val ledgerSelectionPurpose: LedgerSelectionPurpose
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<Int>(ARG_NETWORK_ID)),
        checkNotNull(savedStateHandle.get<LedgerSelectionPurpose>(ARG_SELECTION_PURPOSE))
    )
}

fun NavController.chooseLedger(
    networkId: Int = Constants.USE_CURRENT_NETWORK,
    ledgerSelectionPurpose: LedgerSelectionPurpose = LedgerSelectionPurpose.CreateAccount
) {
    navigate(
        route = "route_choose_ledger?$ARG_NETWORK_ID=$networkId&$ARG_SELECTION_PURPOSE=$ledgerSelectionPurpose"
    )
}

fun NavGraphBuilder.chooseLedger(
    onBackClick: () -> Unit,
    onFinish: () -> Unit,
    onStartRecovery: (FactorSource, Boolean) -> Unit
) {
    markAsHighPriority(ROUTE_CHOOSE_LEDGER)
    composable(
        route = ROUTE_CHOOSE_LEDGER,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        arguments = listOf(
            navArgument(ARG_NETWORK_ID) {
                type = NavType.IntType
                defaultValue = Constants.USE_CURRENT_NETWORK
            },
            navArgument(ARG_SELECTION_PURPOSE) {
                type = NavType.EnumType(LedgerSelectionPurpose::class.java)
                defaultValue = LedgerSelectionPurpose.CreateAccount
            }
        )
    ) {
        ChooseLedgerScreen(
            viewModel = hiltViewModel(),
            addLedgerDeviceViewModel = hiltViewModel(),
            addLinkConnectorViewModel = hiltViewModel(),
            onBackClick = onBackClick,
            goBackToCreateAccount = onFinish,
            onStartRecovery = onStartRecovery
        )
    }
}

enum class LedgerSelectionPurpose {
    CreateAccount, RecoveryScanOlympia, RecoveryScanBabylon
}
