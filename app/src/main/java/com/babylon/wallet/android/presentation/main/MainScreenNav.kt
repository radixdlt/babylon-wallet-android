package com.babylon.wallet.android.presentation.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress

const val MAIN_ROUTE = "main"

@Suppress("LongParameterList")
fun NavGraphBuilder.main(
    viewModel: MainViewModel,
    onAccountClick: (Account) -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit,
    onNavigateToBootError: () -> Unit,
    showNPSSurvey: () -> Unit,
    onNavigateToRelinkConnectors: () -> Unit,
    onNavigateToConnectCloudBackup: () -> Unit,
    onNavigateToLinkConnector: () -> Unit,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    onDAppClick: (AccountAddress) -> Unit,
    onInfoLinkClick: (GlossaryItem) -> Unit,
    onMoreInfoClick: () -> Unit,
    onMoreBlogPostsClick: () -> Unit
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
            onAccountClick = onAccountClick,
            onAccountCreationClick = onAccountCreationClick,
            onNavigateToOnBoarding = onNavigateToOnBoarding,
            onNavigateToIncompatibleProfile = onNavigateToIncompatibleProfile,
            onNavigateToBootError = onNavigateToBootError,
            onNavigateToSecurityCenter = onNavigateToSecurityCenter,
            showNPSSurvey = showNPSSurvey,
            onNavigateToRelinkConnectors = onNavigateToRelinkConnectors,
            onNavigateToConnectCloudBackup = onNavigateToConnectCloudBackup,
            onNavigateToLinkConnector = onNavigateToLinkConnector,
            onSettingClick = onSettingClick,
            onDAppClick = onDAppClick,
            onInfoLinkClick = onInfoLinkClick,
            onMoreInfoClick = onMoreInfoClick,
            onMoreBlogPostsClick = onMoreBlogPostsClick
        )
    }
}
