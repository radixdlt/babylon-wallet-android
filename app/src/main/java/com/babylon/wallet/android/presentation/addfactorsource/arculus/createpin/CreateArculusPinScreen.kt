package com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.PinEntryField
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.keyboardVisiblePadding
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun CreateArculusPinScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateArculusPinViewModel,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CreateArculusPinContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onPinChange = viewModel::onPinChange,
        onConfirmPinChange = viewModel::onConfirmPinChange,
        onCreateClick = viewModel::onCreateClick,
        onDismissMessage = viewModel::onDismissMessage
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                CreateArculusPinViewModel.Event.PinCreated -> onConfirmed()
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
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        keyboardController?.show()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onDismiss,
                backIconType = BackIconType.Back,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.background,
        bottomBar = {
            RadixBottomBar(
                onClick = onCreateClick,
                text = "Create", // TODO crowdin
                enabled = state.isCreateEnabled,
                isLoading = state.isCreateLoading
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
                text = "Create New PIN", // TODO crowdin
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

            Text(
                text = "Choose a 6-digit PIN for your Arculus Card. You’ll keep this PIN if you use this card with " +
                    "another wallet.", // TODO crowdin
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))

            Column {
                Text(
                    text = "Enter PIN", // TODO crowdin
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                PinEntryField(
                    pinLength = CreateArculusPinViewModel.State.PIN_LENGTH,
                    onPinChange = onPinChange,
                    imeAction = ImeAction.Next
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

                Text(
                    text = "Confirm PIN", // TODO crowdin
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                PinEntryField(
                    pinLength = CreateArculusPinViewModel.State.PIN_LENGTH,
                    onPinChange = onConfirmPinChange,
                    imeAction = ImeAction.Done,
                    isEnabled = state.isConfirmedPinEnabled
                )

                if (state.showPinsNotMatchingError) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                    WarningText(
                        text = AnnotatedString("PINs do not match"),
                        textStyle = RadixTheme.typography.body2HighImportance,
                        contentColor = RadixTheme.colors.error
                    )
                }
            }
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
