
package com.babylon.wallet.android.presentation.settings.recovery

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType

private const val ROUTE = "derive_legacy_account"

fun NavController.deriveLegacyAccount() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.deriveLegacyAccount(
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
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        DeriveLegacyAccountScreen(
            viewModel = hiltViewModel(),
            onBack = onBack,
            onChooseSeedPhrase = onChooseSeedPhrase,
            onChooseLedger = onChooseLedger
        )
    }
}
