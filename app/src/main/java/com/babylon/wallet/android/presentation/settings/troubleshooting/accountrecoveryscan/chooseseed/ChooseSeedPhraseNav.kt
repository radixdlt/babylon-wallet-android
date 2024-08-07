package com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.chooseseed

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType
import com.radixdlt.sargon.FactorSourceId

private const val ARGS_MNEMONIC_TYPE = "mnemonic_type"
private const val ROUTE = "choose_seed_phrase?$ARGS_MNEMONIC_TYPE={$ARGS_MNEMONIC_TYPE}"

internal class ChooseSeedPhraseArgs(val recoveryType: MnemonicType) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(
            savedStateHandle.get<MnemonicType>(
                ARGS_MNEMONIC_TYPE
            )
        ),
    )
}

fun NavController.chooseSeedPhrase(mnemonicType: MnemonicType) {
    navigate(route = "choose_seed_phrase?$ARGS_MNEMONIC_TYPE=$mnemonicType")
}

fun NavGraphBuilder.chooseSeedPhrase(
    onBack: () -> Unit,
    onAddSeedPhrase: (MnemonicType) -> Unit,
    onRecoveryScanWithFactorSource: (FactorSourceId.Hash, Boolean) -> Unit
) {
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(
                name = ARGS_MNEMONIC_TYPE,
            ) {
                defaultValue = MnemonicType.Babylon
            }
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
        ChooseSeedPhraseScreen(
            viewModel = hiltViewModel(),
            onBack = onBack,
            onAddSeedPhrase = onAddSeedPhrase,
            onRecoveryScanWithFactorSource = onRecoveryScanWithFactorSource
        )
    }
}
