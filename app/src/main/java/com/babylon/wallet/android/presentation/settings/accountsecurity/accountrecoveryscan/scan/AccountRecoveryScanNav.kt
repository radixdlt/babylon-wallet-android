package com.babylon.wallet.android.presentation.settings.accountsecurity.accountrecoveryscan.scan

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
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

@VisibleForTesting
internal const val ARG_FACTOR_SOURCE_ID_BODY_HEX = "arg_factor_source_id_body"
internal const val ARG_IS_OLYMPIA = "arg_is_olympia"

private const val ROUTE = "account_recovery_scan?" +
        "$ARG_FACTOR_SOURCE_ID_BODY_HEX={$ARG_FACTOR_SOURCE_ID_BODY_HEX}" +
        "&$ARG_IS_OLYMPIA={$ARG_IS_OLYMPIA}"

internal class AccountRecoveryScanArgs(
    val factorSourceId: FactorSourceId.Hash?,
    val isOlympia: Boolean?
) {
    constructor(savedStateHandle: androidx.lifecycle.SavedStateHandle) : this(
        savedStateHandle.get<String>(ARG_FACTOR_SOURCE_ID_BODY_HEX)?.hexToBagOfBytes()?.let {
            FactorSourceIdFromHash(kind = FactorSourceKind.DEVICE, body = Exactly32Bytes.init(it)).asGeneral()
        },
        savedStateHandle.get<Boolean>(ARG_IS_OLYMPIA),
    )
}

fun NavController.accountRecoveryScan(
    factorSourceId: FactorSourceId.Hash? = null,
    isOlympia: Boolean = false
) {
    navigate(
        route = "account_recovery_scan?" +
                "$ARG_FACTOR_SOURCE_ID_BODY_HEX=${factorSourceId?.value?.body?.hex}" +
                "&$ARG_IS_OLYMPIA=$isOlympia"
    )
}

fun NavGraphBuilder.accountRecoveryScan(
    onBackClick: () -> Unit,
    onRecoveryComplete: () -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        arguments = listOf(
            navArgument(ARG_FACTOR_SOURCE_ID_BODY_HEX) {
                defaultValue = null
                nullable = true
                type = NavType.StringType
            },
            navArgument(ARG_IS_OLYMPIA) {
                defaultValue = false
                type = NavType.BoolType
            }
        )
    ) {
        AccountRecoveryScanScreen(
            onBackClick = onBackClick,
            viewModel = hiltViewModel(),
            onRecoveryComplete = onRecoveryComplete
        )
    }
}
