package com.babylon.wallet.android.presentation.accessfactorsources.authorization

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessContent
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessContentRetryButton
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.none
import com.radixdlt.sargon.AuthorizationPurpose
import kotlinx.coroutines.launch

@Composable
fun RequestAuthorizationDialog(
    modifier: Modifier = Modifier,
    viewModel: RequestAuthorizationViewModel,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                RequestAuthorizationViewModel.Event.Completed -> onDismiss()
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    RequestAuthorizationBottomSheetContent(
        modifier = modifier,
        state = state,
        onRetry = viewModel::onRetry,
        onDismiss = viewModel::onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestAuthorizationBottomSheetContent(
    modifier: Modifier = Modifier,
    state: RequestAuthorizationViewModel.State,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            sheetState.show()
        }
    }

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = sheetState,
        heightFraction = 0.8f,
        onDismissRequest = onDismiss,
        sheetContent = {
            Scaffold(
                topBar = {
                    RadixCenteredTopAppBar(
                        windowInsets = WindowInsets.none,
                        title = "",
                        onBackClick = onDismiss,
                        backIconType = BackIconType.Close
                    )
                },
                containerColor = RadixTheme.colors.defaultBackground,
            ) { padding ->
                AccessContent(
                    modifier = Modifier.fillMaxWidth().padding(padding),
                    title = when (state.purpose) {
                        AuthorizationPurpose.CREATING_ACCOUNT,
                        AuthorizationPurpose.CREATING_ACCOUNTS -> stringResource(R.string.authorization_createAccount_title)
                        AuthorizationPurpose.CREATING_PERSONA,
                        AuthorizationPurpose.CREATING_PERSONAS -> stringResource(R.string.authorization_createPersona_title)
                    },
                    message = AnnotatedString(
                        text = stringResource(R.string.authorization_createEntity_message)
                    )
                ) {
                    AccessContentRetryButton(
                        isEnabled = state.isRetryEnabled,
                        onClick = onRetry
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun RequestAuthorizationCreatingAccountPreview() {
    RadixWalletPreviewTheme {
        RequestAuthorizationBottomSheetContent(
            state = RequestAuthorizationViewModel.State(
                purpose = AuthorizationPurpose.CREATING_ACCOUNT,
                isRequestingAuthorization = false
            ),
            onRetry = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun RequestAuthorizationCreatingPersonaPreview() {
    RadixWalletPreviewTheme {
        RequestAuthorizationBottomSheetContent(
            state = RequestAuthorizationViewModel.State(
                purpose = AuthorizationPurpose.CREATING_PERSONA,
                isRequestingAuthorization = false
            ),
            onRetry = {},
            onDismiss = {}
        )
    }
}
