package com.babylon.wallet.android.presentation.onboarding.restore.mnemonic

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

private const val ARGS_FACTOR_SOURCE_ID = "factor_source_id"
private const val ARGS_MNEMONIC_TYPE = "mnemonic_type"
const val ROUTE_ADD_SINGLE_MNEMONIC =
    "add_single_mnemonic?$ARGS_FACTOR_SOURCE_ID={$ARGS_FACTOR_SOURCE_ID}&${ARGS_MNEMONIC_TYPE}={$ARGS_MNEMONIC_TYPE}"

fun NavController.addSingleMnemonic(
    id: String? = null,
    mnemonicType: MnemonicType = MnemonicType.Babylon
) {
    navigate(route = "add_single_mnemonic?$ARGS_FACTOR_SOURCE_ID=$id&${ARGS_MNEMONIC_TYPE}=$mnemonicType")
}

internal class AddSingleMnemonicNavArgs(
    val factorSourceId: String?,
    val mnemonicType: MnemonicType = MnemonicType.Babylon
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<String>(
            ARGS_FACTOR_SOURCE_ID
        ),
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

fun NavGraphBuilder.addSingleMnemonic(
    onBackClick: () -> Unit,
    onStartRecovery: () -> Unit
) {
    markAsHighPriority(ROUTE_ADD_SINGLE_MNEMONIC)
    composable(
        route = ROUTE_ADD_SINGLE_MNEMONIC,
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
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        AddSingleMnemonicScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onStartRecovery = onStartRecovery
        )
    }
}
