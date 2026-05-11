package com.babylon.wallet.android.presentation.settings.securitycenter.mfafactorinstance

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.radixdlt.sargon.FactorSourceId

const val ROUTE_MFA_FACTOR_INSTANCE = "route_mfa_factor_instance"

fun NavController.mfaFactorInstance() {
    navigate(ROUTE_MFA_FACTOR_INSTANCE)
}

fun NavGraphBuilder.mfaFactorInstance(
    toAddressDetails: (ActionableAddress) -> Unit,
    toFactorSourceDetails: (FactorSourceId) -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_MFA_FACTOR_INSTANCE,
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
        MfaFactorInstanceScreen(
            viewModel = hiltViewModel(),
            toAddressDetails = toAddressDetails,
            toFactorSourceDetails = toFactorSourceDetails,
            onBackClick = onBackClick
        )
    }
}
