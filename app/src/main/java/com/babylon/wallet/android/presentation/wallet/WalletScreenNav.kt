import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.wallet.WalletScreen
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network

fun NavController.wallet() {
    navigate("wallet")
}

fun NavGraphBuilder.wallet(
    onMenuClick: () -> Unit,
    onAccountClick: (Network.Account) -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToMnemonicBackup: (FactorSource.FactorSourceID.FromHash) -> Unit,
    onNavigateToMnemonicRestore: () -> Unit
) {
    composable(
        route = "wallet",
        enterTransition = {
            null
        },
        exitTransition = {
            null
        },
        popExitTransition = {
            null
        },
        popEnterTransition = {
            null
        }
    ) {
        WalletScreen(
            viewModel = hiltViewModel(),
            onMenuClick = onMenuClick,
            onAccountClick = onAccountClick,
            onAccountCreationClick = onAccountCreationClick,
            onNavigateToMnemonicBackup = onNavigateToMnemonicBackup,
            onNavigateToMnemonicRestore = onNavigateToMnemonicRestore
        )
    }
}
