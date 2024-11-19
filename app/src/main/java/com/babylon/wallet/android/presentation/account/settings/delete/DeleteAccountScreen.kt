package com.babylon.wallet.android.presentation.account.settings.delete

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet

@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel,
    modifier: Modifier = Modifier,
    onMoveAssetsToAnotherAccount: (AccountAddress) -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is DeleteAccountViewModel.Event.MoveAssetsToAnotherAccount -> onMoveAssetsToAnotherAccount(event.deletingAccountAddress)
            }
        }
    }

    DeleteAccountContent(
        modifier = modifier,
        state = state,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onMessageShown = viewModel::onMessageShown,
        onDismiss = onDismiss
    )
}

@Composable
private fun DeleteAccountContent(
    modifier: Modifier = Modifier,
    state: DeleteAccountViewModel.State,
    onDeleteConfirm: () -> Unit,
    onMessageShown: () -> Unit,
    onDismiss: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
                windowInsets = WindowInsets.statusBarsAndBanner,
                onBackClick = onDismiss
            )
        },
        bottomBar = {
            RadixBottomBar(
                button = {
                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(R.string.common_continue),
                        isLoading = state.isContinueLoading,
                        onClick = {
                            if (!state.isContinueLoading) {
                                onDeleteConfirm()
                            }
                        }
                    )
                },
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Icon(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                painter = painterResource(com.babylon.wallet.android.designsystem.R.drawable.ic_account_delete),
                contentDescription = null,
                tint = RadixTheme.colors.gray2
            )

            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = RadixTheme.dimensions.paddingXXXLarge)
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                text = stringResource(id = R.string.accountSettings_deleteAccount_title),
                style = RadixTheme.typography.title,
                textAlign = TextAlign.Center
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RadixTheme.dimensions.paddingLarge)
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = stringResource(id = R.string.accountSettings_deleteAccount_message),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1HighImportance
            )
        }
    }
}

@UsesSampleValues
@Composable
@Preview
fun DeleteAccountPreview() {
    RadixWalletPreviewTheme {
        DeleteAccountContent(
            state = DeleteAccountViewModel.State(accountAddress = AccountAddress.sampleMainnet()),
            onDeleteConfirm = {},
            onMessageShown = {},
            onDismiss = {}
        )
    }
}
