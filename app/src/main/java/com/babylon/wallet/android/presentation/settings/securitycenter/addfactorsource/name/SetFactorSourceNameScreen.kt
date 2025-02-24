package com.babylon.wallet.android.presentation.settings.securitycenter.addfactorsource.name

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
import androidx.compose.material3.Icon
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
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
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

@Composable
private fun SetFactorSourceNameContent(
    modifier: Modifier = Modifier,
    state: SetFactorSourceNameViewModel.State,
    onDismiss: () -> Unit,
    onDismissMessage: () -> Unit,
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
        containerColor = RadixTheme.colors.defaultBackground,
        bottomBar = {
            RadixBottomBar(
                modifier = Modifier.imePadding(),
                onClick = onSaveClick,
                text = stringResource(id = R.string.common_save),
                insets = if (isKeyboardVisible()) WindowInsets.none else WindowInsets.navigationBars,
                enabled = state.isButtonEnabled
            )
        }
    ) { padding ->
        val inputFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            inputFocusRequester.requestFocus()
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
                tint = RadixTheme.colors.gray1
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = state.factorSourceKind.nameTitle(),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
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
                hintColor = RadixTheme.colors.gray2,
                hint = "Enter name", // TODO crowdin
                error = stringResource(id = R.string.renameLabel_factorSource_tooLong).takeIf { state.isNameTooLong }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

            Text(
                text = "This can be changed any time", // TODO crowdin
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2,
            )
        }
    }

    state.errorMessage?.let { error ->
        BasicPromptAlertDialog(
            finish = { onDismissMessage() },
            messageText = error.getMessage(),
            confirmText = "Close", // TODO crowdin
            dismissText = null
        )
    }
}

@Composable
private fun FactorSourceKind.nameTitle() = when (this) {
    FactorSourceKind.DEVICE -> "Name your New Biometrics/PIN Seed Phrase"
    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> "Name your New Ledger Nano"
    FactorSourceKind.OFF_DEVICE_MNEMONIC -> "Name your New Mnemonic Seed Phrase"
    FactorSourceKind.ARCULUS_CARD -> "Name your New Arculus Card"
    FactorSourceKind.PASSWORD -> "Name your New Password"
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
        }.asSequence()
}
