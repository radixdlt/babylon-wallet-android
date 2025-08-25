package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.verifypin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.PinTextField
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.keyboardVisiblePadding
import com.babylon.wallet.android.utils.Constants.ARCULUS_PIN_LENGTH
import com.radixdlt.sargon.FactorSourceId
import kotlinx.coroutines.delay

@Composable
fun VerifyArculusPinScreen(
    viewModel: VerifyArculusPinViewModel,
    onDismiss: () -> Unit,
    onComplete: (FactorSourceId, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    VerifyArculusPinContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onPinChange = viewModel::onPinChange,
        onContinueClick = viewModel::onContinueClick,
        onDismissMessage = viewModel::onDismissMessage
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is VerifyArculusPinViewModel.Event.Complete -> onComplete(event.factorSourceId, event.pin)
            }
        }
    }
}

@Composable
private fun VerifyArculusPinContent(
    state: VerifyArculusPinViewModel.State,
    onDismiss: () -> Unit,
    onPinChange: (String) -> Unit,
    onContinueClick: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pinFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(300)
        pinFocusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.arculusDetails_verifyPin_title),
                onBackClick = onDismiss,
                backIconType = BackIconType.Back,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.background,
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = stringResource(R.string.common_continue),
                enabled = state.isContinueEnabled,
                isLoading = state.isContinueLoading
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .keyboardVisiblePadding(
                    padding = padding,
                    bottom = RadixTheme.dimensions.paddingDefault
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.arculusDetails_verifyPin_description),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

            val focusManager = LocalFocusManager.current

            PinTextField(
                textFieldModifier = Modifier.focusRequester(pinFocusRequester),
                title = stringResource(id = R.string.arculusDetails_verifyPin_inputLabel),
                pinValue = state.pin,
                pinLength = ARCULUS_PIN_LENGTH,
                onPinChange = onPinChange,
                onPinComplete = { focusManager.clearFocus() }
            )
        }
    }

    state.errorMessage?.let { error ->
        ErrorAlertDialog(
            cancel = onDismissMessage,
            errorMessage = error,
            cancelMessage = stringResource(id = R.string.common_close)
        )
    }
}

@Composable
@Preview
private fun VerifyArculusPinPreview() {
    RadixWalletPreviewTheme {
        VerifyArculusPinContent(
            state = VerifyArculusPinViewModel.State(
                pin = "123456"
            ),
            onDismiss = {},
            onPinChange = {},
            onContinueClick = {},
            onDismissMessage = {}
        )
    }
}
