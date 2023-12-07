@file:OptIn(ExperimentalAnimationApi::class)

package com.babylon.wallet.android.presentation.settings.recovery.chooseseed

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType
import rdx.works.profile.data.model.factorsources.FactorSource

private const val ARGS_MNEMONIC_TYPE = "mnemonic_type"
private const val ROUTE = "choose_olympia_seed?$ARGS_MNEMONIC_TYPE={$ARGS_MNEMONIC_TYPE}"

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
    navigate(route = "choose_olympia_seed?$ARGS_MNEMONIC_TYPE=$mnemonicType")
}

fun NavGraphBuilder.chooseSeedPhrase(
    onBack: () -> Unit,
    onAddSeedPhrase: (MnemonicType) -> Unit,
    onRecoveryScanWithFactorSource: (FactorSource, Boolean) -> Unit
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
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        popEnterTransition = {
            EnterTransition.None
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
