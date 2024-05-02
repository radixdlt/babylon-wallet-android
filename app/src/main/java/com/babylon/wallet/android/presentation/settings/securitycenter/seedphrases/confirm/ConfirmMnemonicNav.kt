package com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.confirm

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson

private const val ARGS_FACTOR_SOURCE_ID = "factorSourceId"
private const val ARGS_MNEMONIC_SIZE = "mnemonicSize"
private const val ROUTE = "confirm_mnemonic?$ARGS_FACTOR_SOURCE_ID={$ARGS_FACTOR_SOURCE_ID}" +
    "&$ARGS_MNEMONIC_SIZE={$ARGS_MNEMONIC_SIZE}"

fun NavController.confirmSeedPhrase(
    factorSourceId: FactorSourceId.Hash,
    mnemonicSize: Int
) {
    navigate(route = "confirm_mnemonic?$ARGS_FACTOR_SOURCE_ID=${factorSourceId.toJson()}&$ARGS_MNEMONIC_SIZE=$mnemonicSize")
}

internal class ConfirmSeedPhraseArgs(
    val factorSourceId: FactorSourceId.Hash,
    val mnemonicSize: Int
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        FactorSourceId.Hash.fromJson(checkNotNull(savedStateHandle.get<String>(ARGS_FACTOR_SOURCE_ID))),
        checkNotNull(savedStateHandle.get<Int>(ARGS_MNEMONIC_SIZE)),
    )
}

fun NavGraphBuilder.confirmSeedPhrase(
    onMnemonicBackedUp: () -> Unit,
    onDismiss: () -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(
                name = ARGS_FACTOR_SOURCE_ID,
            ) {
                nullable = false
                type = NavType.StringType
            },
            navArgument(
                name = ARGS_MNEMONIC_SIZE,
            ) {
                nullable = false
                type = NavType.IntType
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        ConfirmMnemonicScreen(
            viewModel = hiltViewModel(),
            onMnemonicBackedUp = onMnemonicBackedUp,
            onDismiss = onDismiss
        )
    }
}
