package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

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
private const val ROUTE = "restore_mnemonics?factorSourceId={$ARG_FACTOR_SOURCE_ID}"

fun NavController.restoreMnemonics(deviceFactorSourceId: FactorSource.FactorSourceID.FromHash? = null) {
    navigate(
        route = deviceFactorSourceId?.let {
            "restore_mnemonics?factorSourceId=${it.body.value}"
        } ?: "restore_mnemonics"
    )
}

sealed class RestoreMnemonicsArgs {
    object RestoreProfile : RestoreMnemonicsArgs()
    data class RestoreSpecificMnemonic(val factorSourceIdHex: String) : RestoreMnemonicsArgs()

    companion object {
        fun from(savedStateHandle: SavedStateHandle): RestoreMnemonicsArgs {
            val factorSourceIdHex: String? = savedStateHandle[ARG_FACTOR_SOURCE_ID]
            return if (factorSourceIdHex != null) {
                RestoreSpecificMnemonic(factorSourceIdHex)
            } else {
                RestoreProfile
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.restoreMnemonicsScreen(
    onFinish: (Boolean) -> Unit
) {
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(
                name = ARG_FACTOR_SOURCE_ID,
            ) {
                nullable = true
                type = NavType.StringType
            }
        )
    ) {
        RestoreMnemonicsScreen(
            viewModel = hiltViewModel(),
            onFinish = onFinish
        )
    }
}
