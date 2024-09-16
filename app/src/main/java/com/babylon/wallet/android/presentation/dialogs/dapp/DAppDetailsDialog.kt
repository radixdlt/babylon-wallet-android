package com.babylon.wallet.android.presentation.dialogs.dapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.DappDetails
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.displayName
import rdx.works.core.domain.resources.Resource

@Composable
fun DAppDetailsDialog(
    viewModel: DAppDetailsDialogViewModel,
    onFungibleClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                DAppDetailsDialogViewModel.Event.DAppDeleted -> onDismiss()
            }
        }
    }
    DAppDetailsDialogContent(
        state = state,
        onMessageShown = viewModel::onMessageShown,
        onFungibleClick = onFungibleClick,
        onNonFungibleClick = onNonFungibleClick,
        onDismiss = onDismiss,
        onDeleteDapp = viewModel::onDeleteDapp,
        onShowLockerDepositsCheckedChange = viewModel::onShowLockerDepositsCheckedChange
    )
}

@Composable
private fun DAppDetailsDialogContent(
    modifier: Modifier = Modifier,
    state: DAppDetailsDialogViewModel.State,
    onFungibleClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit,
    onMessageShown: () -> Unit,
    onDismiss: () -> Unit,
    onDeleteDapp: () -> Unit,
    onShowLockerDepositsCheckedChange: (Boolean) -> Unit
) {
    var showDeleteDappPrompt by remember { mutableStateOf(false) }
    if (showDeleteDappPrompt) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    onDeleteDapp()
                }
                showDeleteDappPrompt = false
            },
            title = {
                Text(
                    text = stringResource(id = R.string.authorizedDapps_forgetDappAlert_title),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            },
            message = {
                Text(
                    text = stringResource(id = R.string.authorizedDapps_forgetDappAlert_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.authorizedDapps_forgetDappAlert_forget)
        )
    }
    BottomSheetDialogWrapper(
        modifier = modifier,
        title = state.dAppWithResources?.dApp.displayName(),
        showDragHandle = true,
        onDismiss = onDismiss
    ) {
        Box(modifier = Modifier.fillMaxHeight(fraction = 0.9f)) {
            DappDetails(
                modifier = Modifier.fillMaxSize(),
                dAppWithResources = state.dAppWithResources,
                isValidatingWebsite = state.isWebsiteValidating,
                validatedWebsite = state.validatedWebsite,
                personaList = state.authorizedPersonas,
                isShowLockerDepositsChecked = state.isShowLockerDepositsChecked,
                isReadOnly = state.isReadOnly,
                onFungibleTokenClick = onFungibleClick,
                onNonFungibleClick = onNonFungibleClick,
                onDeleteDapp = {
                    showDeleteDappPrompt = true
                },
                onPersonaClick = null,
                onShowLockerDepositsCheckedChange = onShowLockerDepositsCheckedChange
            )

            SnackbarUiMessageHandler(
                message = state.uiMessage,
                onMessageShown = onMessageShown
            )
        }
    }
}
