package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable
import rdx.works.profile.data.model.factorsources.DeviceFactorSource

private const val ARG_FACTOR_SOURCE_ID = "factorSourceId"
private const val ROUTE = "restore_mnemonics?factorSourceId={$ARG_FACTOR_SOURCE_ID}"

fun NavController.restoreMnemonics(deviceFactorSource: DeviceFactorSource? = null) {
    navigate(
        route = deviceFactorSource?.let {
            "restore_mnemonics?factorSourceId=${it.id.body.value}"
        } ?: "restore_mnemonics"
    )
}

internal class RestoreMnemonicsArgs(val factorSourceIdHex: String?) {
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle[ARG_FACTOR_SOURCE_ID])
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.restoreMnemonicsScreen(
    onFinish: () -> Unit
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
