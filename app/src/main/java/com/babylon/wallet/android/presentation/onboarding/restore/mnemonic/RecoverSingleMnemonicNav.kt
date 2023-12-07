package com.babylon.wallet.android.presentation.onboarding.restore.mnemonic

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.account.recover.AccountRecoveryViewModel
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

private const val ARGS_FACTOR_SOURCE_ID = "factor_source_id"
private const val ARGS_MNEMONIC_TYPE = "mnemonic_type"
const val ROUTE_RECOVER_SINGLE_MNEMONIC =
    "recover_single_mnemonic?$ARGS_FACTOR_SOURCE_ID={$ARGS_FACTOR_SOURCE_ID}&${ARGS_MNEMONIC_TYPE}={$ARGS_MNEMONIC_TYPE}"

fun NavController.recoverSingleMnemonic(
    id: String? = null,
    mnemonicType: MnemonicType = MnemonicType.Babylon
) {
    navigate(route = "recover_single_mnemonic?$ARGS_FACTOR_SOURCE_ID=$id&${ARGS_MNEMONIC_TYPE}=$mnemonicType")
}

internal class RestoreSingleMnemonicNavArgs(
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

fun NavGraphBuilder.recoverSingleMnemonic(
    navController: NavController,
    onBackClick: () -> Unit,
    onStartRecovery: () -> Unit
) {
    markAsHighPriority(ROUTE_RECOVER_SINGLE_MNEMONIC)
    composable(
        route = ROUTE_RECOVER_SINGLE_MNEMONIC,
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
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_RECOVER_SINGLE_MNEMONIC)
        }
        val sharedViewModel = hiltViewModel<AccountRecoveryViewModel>(parentEntry)
        RecoverSingleMnemonicScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            sharedViewModel = sharedViewModel,
            onStartRecovery = onStartRecovery
        )
    }
}
