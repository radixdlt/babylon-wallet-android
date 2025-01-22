package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldname

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun SetupShieldNameScreen(
    viewModel: SetupShieldNameViewModel,
    onDismiss: () -> Unit,
    onShieldCreated: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SetupShieldNameContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onMessageShown = viewModel::onMessageShown,
        onConfirmClick = viewModel::onConfirmClick,
        onDismiss = onDismiss
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SetupShieldNameViewModel.Event.ShieldCreated -> onShieldCreated()
            }
        }
    }
}

@Composable
private fun SetupShieldNameContent(
    modifier: Modifier = Modifier,
    state: SetupShieldNameViewModel.State,
    onNameChange: (String) -> Unit,
    onMessageShown: () -> Unit,
    onConfirmClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.message,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onDismiss,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onConfirmClick,
                text = stringResource(R.string.common_confirm),
                insets = WindowInsets.navigationBars.union(WindowInsets.ime),
                enabled = state.isButtonEnabled
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.white
    ) { padding ->
        val inputFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            inputFocusRequester.requestFocus()
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                text = stringResource(id = R.string.shieldWizardName_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                text = stringResource(id = R.string.shieldWizardName_subtitle),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge)
                    .focusRequester(focusRequester = inputFocusRequester),
                onValueChanged = onNameChange,
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words),
                value = state.name,
                singleLine = true,
                hintColor = RadixTheme.colors.gray2,
                error = stringResource(id = R.string.shieldWizardName_tooLong).takeIf { state.isNameTooLong }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
        }
    }
}

@Composable
@Preview
private fun SetupShieldNamePreview(
    @PreviewParameter(SetupShieldNamePreviewProvider::class) state: SetupShieldNameViewModel.State
) {
    RadixWalletPreviewTheme {
        SetupShieldNameContent(
            state = state,
            onNameChange = {},
            onMessageShown = {},
            onConfirmClick = {},
            onDismiss = {}
        )
    }
}

class SetupShieldNamePreviewProvider : PreviewParameterProvider<SetupShieldNameViewModel.State> {

    override val values: Sequence<SetupShieldNameViewModel.State>
        get() = sequenceOf(
            SetupShieldNameViewModel.State(),
            SetupShieldNameViewModel.State(
                name = "Security shield"
            )
        )
}
