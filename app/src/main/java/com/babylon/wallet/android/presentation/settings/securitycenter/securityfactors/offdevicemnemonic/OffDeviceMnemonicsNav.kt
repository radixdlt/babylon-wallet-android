package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.offdevicemnemonic

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.radixdlt.sargon.FactorSourceId

private const val ROUTE_OFF_DEVICE_MNEMONICS_SCREEN = "route_off_device_mnemonics_screen"

fun NavController.offDeviceMnemonics() {
    navigate(ROUTE_OFF_DEVICE_MNEMONICS_SCREEN) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.offDeviceMnemonics(
    onNavigateToOffDeviceMnemonicFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onNavigateToOffDeviceAddMnemonic: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_OFF_DEVICE_MNEMONICS_SCREEN,
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
        OffDeviceMnemonicsScreen(
            viewModel = hiltViewModel(),
            onNavigateToOffDeviceMnemonicFactorSourceDetails = onNavigateToOffDeviceMnemonicFactorSourceDetails,
            onNavigateToAddOffDeviceMnemonic = onNavigateToOffDeviceAddMnemonic,
            onInfoClick = onInfoClick,
            onBackClick = onBackClick
        )
    }
}
