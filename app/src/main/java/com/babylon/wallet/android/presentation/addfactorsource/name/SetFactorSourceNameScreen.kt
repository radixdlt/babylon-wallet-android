package com.babylon.wallet.android.presentation.addfactorsource.name

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.themedColorFilter
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.iconRes
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.isKeyboardVisible
import com.babylon.wallet.android.presentation.ui.modifier.keyboardVisiblePadding
import com.babylon.wallet.android.presentation.ui.none
import com.radixdlt.sargon.FactorSourceKind

@Composable
fun SetFactorSourceNameScreen(
    viewModel: SetFactorSourceNameViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SetFactorSourceNameContent(
        state = state,
        onDismiss = onDismiss,
        onDismissMessage = viewModel::onDismissMessage,
        onDismissSuccessMessage = viewModel::onDismissSuccessMessage,
        onNameChange = viewModel::onNameChange,
        onSaveClick = viewModel::onSaveClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                SetFactorSourceNameViewModel.Event.Saved -> onSaved()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetFactorSourceNameContent(
    modifier: Modifier = Modifier,
    state: SetFactorSourceNameViewModel.State,
    onDismiss: () -> Unit,
    onDismissMessage: () -> Unit,
    onDismissSuccessMessage: () -> Unit,
    onNameChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = { onDismiss() },
                backIconType = BackIconType.Back,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.background,
        bottomBar = {
            RadixBottomBar(
                modifier = Modifier.imePadding(),
                onClick = onSaveClick,
                text = stringResource(id = R.string.newBiometricFactor_name_saveButton),
                insets = if (isKeyboardVisible()) WindowInsets.none else WindowInsets.navigationBars,
                enabled = state.isButtonEnabled,
                isLoading = state.saveInProgress
            )
        }
    ) { padding ->
        val inputFocusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            inputFocusRequester.requestFocus()
        }

        LaunchedEffect(state.saveInProgress) {
            if (state.saveInProgress) {
                inputFocusRequester.freeFocus()
                keyboardController?.hide()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .keyboardVisiblePadding(
                    padding = padding,
                    bottom = RadixTheme.dimensions.buttonDefaultHeight + RadixTheme.dimensions.paddingXXLarge
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
        ) {
            Icon(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = state.factorSourceKind.iconRes()),
                contentDescription = null,
                tint = RadixTheme.colors.icon
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = state.factorSourceKind.nameTitle(),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester = inputFocusRequester),
                onValueChanged = onNameChange,
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words),
                value = state.name,
                singleLine = true,
                hint = stringResource(id = R.string.newBiometricFactor_name_label),
                error = stringResource(id = R.string.renameLabel_factorSource_tooLong).takeIf { state.isNameTooLong }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

            Text(
                text = stringResource(id = R.string.newBiometricFactor_name_note),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary,
            )
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.showSuccess) {
        if (state.showSuccess) {
            bottomSheetState.show()
        }
    }

    if (state.showSuccess) {
        DefaultModalSheetLayout(
            wrapContent = true,
            sheetState = bottomSheetState,
            sheetContent = {
                SuccessSheetContent(
                    onCloseClick = onDismissSuccessMessage
                )
            },
            onDismissRequest = onDismissSuccessMessage
        )
    }

    state.errorMessage?.let { error ->
        ErrorAlertDialog(
            cancel = onDismissMessage,
            errorMessage = error
        )
    }
}

@Composable
private fun FactorSourceKind.nameTitle() = when (this) {
    FactorSourceKind.DEVICE -> stringResource(id = R.string.newBiometricFactor_intro_title)
    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> stringResource(id = R.string.addFactorSource_ledger_nameTitle)
    FactorSourceKind.OFF_DEVICE_MNEMONIC -> stringResource(id = R.string.addFactorSource_offDeviceMnemonic_nameTitle)
    FactorSourceKind.ARCULUS_CARD -> stringResource(id = R.string.addFactorSource_arculus_nameTitle)
    FactorSourceKind.PASSWORD -> stringResource(id = R.string.addFactorSource_password_nameTitle)
}

@Composable
private fun SuccessSheetContent(
    onCloseClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        RadixCenteredTopAppBar(
            title = "",
            backIconType = BackIconType.Close,
            onBackClick = onCloseClick,
            windowInsets = WindowInsets(0.dp)
        )

        Image(
            painter = painterResource(DSR.check_circle_outline),
            contentDescription = null,
            colorFilter = themedColorFilter()
        )

        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.newFactor_success_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )

        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.newFactor_success_subtitle),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )

        RadixBottomBar(
            onClick = onCloseClick,
            text = stringResource(id = R.string.common_close),
            insets = WindowInsets.none
        )
    }
}

@Composable
@Preview
private fun SetFactorSourceNamePreview(
    @PreviewParameter(SetFactorSourceNamePreviewProvider::class) state: SetFactorSourceNameViewModel.State
) {
    RadixWalletPreviewTheme {
        SetFactorSourceNameContent(
            state = state,
            onDismiss = {},
            onDismissMessage = {},
            onDismissSuccessMessage = {},
            onNameChange = {},
            onSaveClick = {}
        )
    }
}

class SetFactorSourceNamePreviewProvider : PreviewParameterProvider<SetFactorSourceNameViewModel.State> {

    override val values: Sequence<SetFactorSourceNameViewModel.State>
        get() = FactorSourceKind.entries.map { kind ->
            SetFactorSourceNameViewModel.State(
                factorSourceKind = kind
            )
        }.asSequence() + SetFactorSourceNameViewModel.State(
            factorSourceKind = FactorSourceKind.DEVICE,
            showSuccess = true
        )
}
