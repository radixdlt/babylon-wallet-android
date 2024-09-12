package com.babylon.wallet.android.presentation.account.createaccount.withledger

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
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.radixdlt.sargon.FactorSourceId

@VisibleForTesting
const val ARG_SELECTION_PURPOSE = "arg_selection_purpose"

private const val ROUTE_CHOOSE_LEDGER = "route_choose_ledger?$ARG_SELECTION_PURPOSE={$ARG_SELECTION_PURPOSE}"

internal class ChooserLedgerArgs(
    val ledgerSelectionPurpose: LedgerSelectionPurpose
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<LedgerSelectionPurpose>(ARG_SELECTION_PURPOSE))
    )
}

fun NavController.chooseLedger(
    ledgerSelectionPurpose: LedgerSelectionPurpose = LedgerSelectionPurpose.DerivePublicKey
) {
    navigate(
        route = "route_choose_ledger?$ARG_SELECTION_PURPOSE=$ledgerSelectionPurpose"
    )
}

fun NavGraphBuilder.chooseLedger(
    onBackClick: () -> Unit,
    onFinish: () -> Unit,
    onStartRecovery: (FactorSourceId.Hash, Boolean) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    markAsHighPriority(ROUTE_CHOOSE_LEDGER)
    composable(
        route = ROUTE_CHOOSE_LEDGER,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        popEnterTransition = {
            EnterTransition.None
        },
        exitTransition = {
            ExitTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        arguments = listOf(
            navArgument(ARG_SELECTION_PURPOSE) {
                type = NavType.EnumType(LedgerSelectionPurpose::class.java)
                defaultValue = LedgerSelectionPurpose.DerivePublicKey
            }
        )
    ) {
        ChooseLedgerScreen(
            viewModel = hiltViewModel(),
            addLedgerDeviceViewModel = hiltViewModel(),
            addLinkConnectorViewModel = hiltViewModel(),
            onBackClick = onBackClick,
            goBackToCreateAccount = onFinish,
            onStartRecovery = onStartRecovery,
            onInfoClick = onInfoClick
        )
    }
}

enum class LedgerSelectionPurpose {
    DerivePublicKey, RecoveryScanOlympia, RecoveryScanBabylon
}
