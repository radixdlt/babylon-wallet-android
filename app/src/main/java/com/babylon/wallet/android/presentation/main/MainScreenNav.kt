package com.babylon.wallet.android.presentation.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.Account

const val MAIN_ROUTE = "main"

@Suppress("LongParameterList")
fun NavGraphBuilder.main(
    viewModel: MainViewModel,
    onMenuClick: () -> Unit,
    onAccountClick: (Account) -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit,
    showNPSSurvey: () -> Unit,
    onNavigateToRelinkConnectors: () -> Unit,
    onNavigateToConnectCloudBackup: () -> Unit,
    onNavigateToLinkConnector: () -> Unit
) {
    composable(
        route = MAIN_ROUTE,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        MainScreen(
            viewModel = viewModel,
            onMenuClick = onMenuClick,
            onAccountClick = onAccountClick,
            onAccountCreationClick = onAccountCreationClick,
            onNavigateToOnBoarding = onNavigateToOnBoarding,
            onNavigateToIncompatibleProfile = onNavigateToIncompatibleProfile,
            onNavigateToSecurityCenter = onNavigateToSecurityCenter,
            showNPSSurvey = showNPSSurvey,
            onNavigateToRelinkConnectors = onNavigateToRelinkConnectors,
            onNavigateToConnectCloudBackup = onNavigateToConnectCloudBackup,
            onNavigateToLinkConnector = onNavigateToLinkConnector
        )
    }
}
