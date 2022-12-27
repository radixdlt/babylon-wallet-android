package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent

@Composable
fun ChooseAccountsScreen(
    viewModel: ChooseAccountsViewModel,
    onBackClick: () -> Unit,
    exitRequestFlow: () -> Unit,
    dismissErrorDialog: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                OneOffEvent.NavigateToCompletionScreen -> {
                    exitRequestFlow()
                }
            }
        }
    }

    viewModel.state.let { state ->
        state.accounts?.let { accounts ->
            ChooseAccountContent(
                onBackClick = onBackClick,
                onContinueClick = {
                    viewModel.sendAccountsResponse()
                },
                imageUrl = state.dAppDetails?.imageUrl.orEmpty(),
                continueButtonEnabled = state.continueButtonEnabled,
                dAppAccounts = accounts,
                accountSelected = viewModel::onAccountSelect
            )
        }

        if (state.showProgress) {
            FullscreenCircularProgressContent()
        }

        state.error?.let { error ->
            DAppAlertDialog(
                title = stringResource(id = R.string.dapp_verification_error_title),
                body = error,
                dismissErrorDialog = dismissErrorDialog
            )
        }
    }
}
