package com.babylon.wallet.android.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.wallet.WalletScreen
import kotlinx.coroutines.flow.StateFlow
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    mainUiState: StateFlow<MainUiState>,
    onMenuClick: () -> Unit,
    onAccountClick: (Network.Account) -> Unit = { },
    onNavigateToMnemonicBackup: (FactorSource.ID) -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit
) {
    val state by mainUiState.collectAsStateWithLifecycle()
    when (state.initialAppState) {
        is AppState.Wallet -> {
            WalletScreen(
                modifier = modifier,
                viewModel = hiltViewModel(),
                onMenuClick = onMenuClick,
                onAccountClick = onAccountClick,
                onAccountCreationClick = onAccountCreationClick,
                onNavigateToMnemonicBackup = onNavigateToMnemonicBackup,
            )
        }
        is AppState.IncompatibleProfile -> {
            LaunchedEffect(state.initialAppState) {
                onNavigateToIncompatibleProfile()
            }
        }
        is AppState.Loading -> {
            FullscreenCircularProgressContent()
        }
        is AppState.OnBoarding -> {
            LaunchedEffect(state.initialAppState) {
                onNavigateToOnBoarding()
            }
        }
        is AppState.NewProfile -> {
            LaunchedEffect(state.initialAppState) {
                onNavigateToCreateAccount()
            }
        }
    }
}
