package com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.configurepin.SetArculusPinScreen
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun CreateArculusPinScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateArculusPinViewModel,
    onDismiss: () -> Unit,
    onConfirmed: (CreateArculusPinContext) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CreateArculusPinContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onPinChange = viewModel::onPinChange,
        onConfirmPinChange = viewModel::onConfirmPinChange,
        onCreateClick = viewModel::onCreateClick,
        onDismissMessage = viewModel::onDismissUiMessage
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is CreateArculusPinViewModel.Event.PinCreated -> onConfirmed(event.context)
            }
        }
    }
}

@Composable
private fun CreateArculusPinContent(
    modifier: Modifier = Modifier,
    state: CreateArculusPinViewModel.State,
    onDismiss: () -> Unit,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onCreateClick: () -> Unit,
    onDismissMessage: () -> Unit
) {
    SetArculusPinScreen(
        modifier = modifier,
        state = state.createPinState,
        topBarTitle = stringResource(id = R.string.empty),
        contentTitle = {
            Text(
                text = stringResource(id = R.string.addArculus_createPin_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
        },
        description = stringResource(id = R.string.addArculus_createPin_description),
        pinInputTitle = stringResource(id = R.string.addArculus_createPin_pinInputLabel),
        confirmPinInputTitle = stringResource(id = R.string.addArculus_createPin_confirmPinInputLabel),
        confirmButtonTitle = stringResource(id = R.string.addArculus_createPin_button),
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
private fun CreateArculusPinPreview() {
    RadixWalletPreviewTheme {
        CreateArculusPinContent(
            state = CreateArculusPinViewModel.State(),
            onDismiss = {},
            onPinChange = {},
            onConfirmPinChange = {},
            onCreateClick = {},
            onDismissMessage = {}
        )
    }
}
