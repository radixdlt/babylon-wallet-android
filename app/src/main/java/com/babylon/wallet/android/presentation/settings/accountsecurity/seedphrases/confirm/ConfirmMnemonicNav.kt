package com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.confirm

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init

private const val ARGS_FACTOR_SOURCE_ID_BODY_HEX = "factorSourceIdBodyHex"
private const val ARGS_MNEMONIC_SIZE = "mnemonicSize"
private const val ROUTE = "confirm_mnemonic?$ARGS_FACTOR_SOURCE_ID_BODY_HEX={$ARGS_FACTOR_SOURCE_ID_BODY_HEX}&$ARGS_MNEMONIC_SIZE={$ARGS_MNEMONIC_SIZE}"

fun NavController.confirmSeedPhrase(
    factorSourceId: FactorSourceId.Hash,
    mnemonicSize: Int
) {
    val idBody = factorSourceId.value.body.hex
    navigate(route = "confirm_mnemonic?$ARGS_FACTOR_SOURCE_ID_BODY_HEX=$idBody&$ARGS_MNEMONIC_SIZE=$mnemonicSize")
}

internal class ConfirmSeedPhraseArgs(
    val factorSourceId: FactorSourceId.Hash,
    val mnemonicSize: Int
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        Exactly32Bytes.init(checkNotNull(savedStateHandle.get<String>(ARGS_FACTOR_SOURCE_ID_BODY_HEX)).hexToBagOfBytes()).let {
            FactorSourceIdFromHash(kind = FactorSourceKind.DEVICE, body = it).asGeneral()
        },
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
                name = ARGS_FACTOR_SOURCE_ID_BODY_HEX,
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
