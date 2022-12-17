package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent

@Composable
fun ChooseAccountsScreen(
    viewModel: ChooseAccountsViewModel,
    onBackClick: () -> Unit,
    onContinueClick: (String) -> Unit,
    dismissErrorDialog: () -> Unit
) {
    viewModel.accountsState.let { accountsState ->
        accountsState.accounts?.let { accounts ->
            ChooseAccountContent(
                onBackClick = onBackClick,
                onContinueClick = {
                    onContinueClick(accountsState.dAppDetails?.dAppName.orEmpty())
                },
                imageUrl = accountsState.dAppDetails?.imageUrl.orEmpty(),
                continueButtonEnabled = accountsState.continueButtonEnabled,
                dAppAccounts = accounts,
                accountSelected = viewModel::onAccountSelect
            )
        }

        if (accountsState.showProgress) {
            FullscreenCircularProgressContent()
        }

        accountsState.error?.let { error ->
            DAppAlertDialog(
                title = stringResource(id = R.string.dapp_verification_error_title),
                body = error,
                dismissErrorDialog = dismissErrorDialog
            )
        }
    }
}
