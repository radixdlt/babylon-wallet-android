package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable
import rdx.works.profile.data.model.factorsources.FactorSource

private const val ARG_FACTOR_SOURCE_ID = "factorSourceId"
private const val ARG_RECOVER_MANDATORY = "mandatory"
private const val ROUTE = "restore_mnemonics?$ARG_FACTOR_SOURCE_ID={$ARG_FACTOR_SOURCE_ID}&$ARG_RECOVER_MANDATORY={$ARG_RECOVER_MANDATORY}"

fun NavController.restoreMnemonics(deviceFactorSourceId: FactorSource.FactorSourceID.FromHash? = null, recoverMandatory: Boolean = false) {
    var route = "restore_mnemonics"
    route += "?$ARG_FACTOR_SOURCE_ID=${deviceFactorSourceId?.body?.value}"
    route += "&$ARG_RECOVER_MANDATORY=$recoverMandatory"
    navigate(route = route)
}

sealed class RestoreMnemonicsArgs {
    object RestoreProfile : RestoreMnemonicsArgs()
    data class RestoreSpecificMnemonic(val factorSourceIdHex: String, val recoverMandatory: Boolean) : RestoreMnemonicsArgs()

    companion object {
        fun from(savedStateHandle: SavedStateHandle): RestoreMnemonicsArgs {
            val factorSourceIdHex: String? = savedStateHandle[ARG_FACTOR_SOURCE_ID]
            val recoverMandatory: Boolean = savedStateHandle[ARG_RECOVER_MANDATORY] ?: false
            return if (factorSourceIdHex != null) {
                RestoreSpecificMnemonic(factorSourceIdHex, recoverMandatory)
            } else {
                RestoreProfile
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.restoreMnemonicsScreen(
    onFinish: (Boolean) -> Unit,
    onCloseApp: () -> Unit
) {
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(
                name = ARG_FACTOR_SOURCE_ID,
            ) {
                nullable = true
                type = NavType.StringType
            },
            navArgument(
                name = ARG_RECOVER_MANDATORY,
            ) {
                nullable = false
                type = NavType.BoolType
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        RestoreMnemonicsScreen(
            viewModel = hiltViewModel(),
            onFinish = onFinish,
            onCloseApp = onCloseApp
        )
    }
}
