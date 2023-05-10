package com.babylon.wallet.android.presentation.main

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import kotlinx.coroutines.flow.StateFlow
import rdx.works.profile.data.model.factorsources.FactorSource

const val MAIN_ROUTE = "main"

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.main(
    mainUiState: StateFlow<MainUiState>,
    onMenuClick: () -> Unit,
    onAccountClick: (accountId: String) -> Unit = { },
    onNavigateToMnemonicBackup: (FactorSource.ID) -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit
) {
    composable(route = MAIN_ROUTE) {
        MainScreen(
            mainUiState = mainUiState,
            onMenuClick = onMenuClick,
            onAccountClick = onAccountClick,
            onAccountCreationClick = onAccountCreationClick,
            onNavigateToCreateAccount = onNavigateToCreateAccount,
            onNavigateToOnBoarding = onNavigateToOnBoarding,
            onNavigateToIncompatibleProfile = onNavigateToIncompatibleProfile,
            onNavigateToMnemonicBackup = onNavigateToMnemonicBackup
        )
    }
}
