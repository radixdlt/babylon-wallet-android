
package com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType

private const val ROUTE = "account_recovery_scan_selection"

fun NavController.accountRecoveryScanSelection() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.accountRecoveryScanSelection(
    onBack: () -> Unit,
    onChooseSeedPhrase: (MnemonicType) -> Unit,
    onChooseLedger: (Boolean) -> Unit
) {
    composable(
        route = ROUTE,
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
        AccountRecoveryScanSelectionScreen(
            viewModel = hiltViewModel(),
            onBack = onBack,
            onChooseSeedPhrase = onChooseSeedPhrase,
            onChooseLedger = onChooseLedger
        )
    }
}
