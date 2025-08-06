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
private const val ARGS_CONTEXT = "context"
private const val ROUTE_IMPORT_SINGLE_MNEMONIC =
    "import_single_mnemonic?$ARGS_FACTOR_SOURCE_ID={$ARGS_FACTOR_SOURCE_ID}&${ARGS_CONTEXT}={$ARGS_CONTEXT}"

fun NavController.importSingleMnemonic(
    context: Context,
    factorSourceId: FactorSourceId? = null
) {
    navigate(
        route = "import_single_mnemonic?" +
            "$ARGS_FACTOR_SOURCE_ID=${factorSourceId?.toJson()}" +
            "&${ARGS_CONTEXT}=$context"
    )
}

internal class ImportSingleMnemonicNavArgs(
    val factorSourceId: FactorSourceId?,
    val context: Context
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<String>(ARGS_FACTOR_SOURCE_ID)?.let {
            FactorSourceId.fromJson(it)
        },
        checkNotNull(savedStateHandle.get<Context>(ARGS_CONTEXT)),
    )
}

enum class Context {

    ImportMainSeedPhrase,
    ImportSeedPhrase
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
            navArgument(name = ARGS_CONTEXT) {
                defaultValue = Context.ImportSeedPhrase
                type = NavType.EnumType(Context::class.java)
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
