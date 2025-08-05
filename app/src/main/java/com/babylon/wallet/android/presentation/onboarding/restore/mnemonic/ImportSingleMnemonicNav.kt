package com.babylon.wallet.android.presentation.onboarding.restore.mnemonic

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
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson

private const val ARGS_FACTOR_SOURCE_ID = "factor_source_id"
private const val ARGS_MNEMONIC_TYPE = "mnemonic_type"
private const val ROUTE_IMPORT_SINGLE_MNEMONIC =
    "import_single_mnemonic?$ARGS_FACTOR_SOURCE_ID={$ARGS_FACTOR_SOURCE_ID}&${ARGS_MNEMONIC_TYPE}={$ARGS_MNEMONIC_TYPE}"

fun NavController.importSingleMnemonic(
    factorSourceId: FactorSourceId? = null,
    mnemonicType: MnemonicType = MnemonicType.Babylon
) {
    navigate(route = "import_single_mnemonic?$ARGS_FACTOR_SOURCE_ID=${factorSourceId?.toJson()}&${ARGS_MNEMONIC_TYPE}=$mnemonicType")
}

internal class AddSingleMnemonicNavArgs(
    val factorSourceId: FactorSourceId?,
    val mnemonicType: MnemonicType = MnemonicType.Babylon
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<String>(
            ARGS_FACTOR_SOURCE_ID
        )?.let { FactorSourceId.fromJson(it) },
        checkNotNull(
            savedStateHandle.get<MnemonicType>(
                ARGS_MNEMONIC_TYPE
            )
        ),
    )
}

enum class MnemonicType {
    Babylon, Olympia, BabylonMain
}

fun NavGraphBuilder.importSingleMnemonic(
    onBackClick: () -> Unit,
    onStartRecovery: () -> Unit
) {
    markAsHighPriority(ROUTE_IMPORT_SINGLE_MNEMONIC)
    composable(
        route = ROUTE_IMPORT_SINGLE_MNEMONIC,
        arguments = listOf(
            navArgument(
                name = ARGS_FACTOR_SOURCE_ID,
            ) {
                nullable = true
                type = NavType.StringType
            },
            navArgument(name = ARGS_MNEMONIC_TYPE) {
                defaultValue = MnemonicType.Babylon
                type = NavType.EnumType(MnemonicType::class.java)
            }
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
        ImportSingleMnemonicScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onStartRecovery = onStartRecovery
        )
    }
}
