package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.changepin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.configurepin.ConfigureArculusPinState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.configurepin.SetArculusPinScreen
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun ChangeArculusPinScreen(
    viewModel: ChangeArculusPinViewModel,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChangeArculusPinContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onPinChange = viewModel::onPinChange,
        onConfirmPinChange = viewModel::onConfirmPinChange,
        onCreateClick = viewModel::onConfirmClick,
        onDismissMessage = viewModel::onDismissUiMessage
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                ChangeArculusPinViewModel.Event.PinChanged -> onComplete()
            }
        }
    }
}

@Composable
private fun ChangeArculusPinContent(
    modifier: Modifier = Modifier,
    state: ChangeArculusPinViewModel.State,
    onDismiss: () -> Unit,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onCreateClick: () -> Unit,
    onDismissMessage: () -> Unit
) {
    SetArculusPinScreen(
        modifier = modifier,
        state = state.createPinState,
        topBarTitle = stringResource(id = R.string.arculusDetails_changePin_title),
        contentTitle = null,
        description = stringResource(id = R.string.arculusDetails_changePin_description),
        pinInputTitle = stringResource(id = R.string.arculusDetails_changePin_newPinInputLabel),
        confirmPinInputTitle = stringResource(id = R.string.arculusDetails_changePin_confirmPinInputLabel),
        confirmButtonTitle = stringResource(R.string.common_continue),
        onDismiss = onDismiss,
        onPinChange = onPinChange,
        onConfirmPinChange = onConfirmPinChange,
        onConfirmClick = onCreateClick,
        onDismissMessage = onDismissMessage
    )
}

@UsesSampleValues
@Composable
@Preview
private fun ChangeArculusPinPreview() {
    RadixWalletPreviewTheme {
        ChangeArculusPinContent(
            state = ChangeArculusPinViewModel.State(
                createPinState = ConfigureArculusPinState(
                    pin = "123456",
                    confirmedPin = "123454"
                )
            ),
            onDismiss = {},
            onPinChange = {},
            onConfirmPinChange = {},
            onCreateClick = {},
            onDismissMessage = {}
        )
    }
}
