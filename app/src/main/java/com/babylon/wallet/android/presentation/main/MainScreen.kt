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
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit,
    onAccountClick: (Network.Account) -> Unit = { },
    onNavigateToMnemonicBackup: (FactorSourceID.FromHash) -> Unit,
    onNavigateToMnemonicRestore: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit,
    onNavigateToWallet: () -> Unit,
    viewModel: MainScreenViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state.initialAppState) {
        is AppState.Wallet -> {
            WalletScreen(
                modifier = modifier,
                viewModel = hiltViewModel(),
                onMenuClick = onMenuClick,
                onAccountClick = onAccountClick,
                onAccountCreationClick = onAccountCreationClick,
                onNavigateToMnemonicBackup = onNavigateToMnemonicBackup,
                onNavigateToMnemonicRestore = onNavigateToMnemonicRestore
            )
        }

        is AppState.IncompatibleProfile -> {
            LaunchedEffect(Unit) {
                onNavigateToIncompatibleProfile()
            }
        }

        is AppState.Loading -> {
            FullscreenCircularProgressContent()
        }

        is AppState.OnBoarding -> {
            LaunchedEffect(Unit) {
                onNavigateToOnBoarding()
            }
        }
    }
}
