package com.babylon.wallet.android.presentation.account.createaccount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.NoMnemonicAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.isKeyboardVisible
import com.radixdlt.sargon.AccountAddress

@Composable
fun CreateAccountScreen(
    viewModel: CreateAccountViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueClick: (
        accountId: AccountAddress,
        requestSource: CreateAccountRequestSource?,
    ) -> Unit = { _: AccountAddress, _: CreateAccountRequestSource? -> }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(onBack = viewModel::onBackClick)

    CreateAccountContent(
        onAccountNameChange = viewModel::onAccountNameChange,
        onAccountCreateClick = viewModel::onAccountCreateClick,
        onBackClick = viewModel::onBackClick,
        modifier = modifier,
        state = state,
        uiMessage = state.uiMessage,
        onUiMessageShown = viewModel::onUiMessageShown
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is CreateAccountEvent.Complete -> onContinueClick(
                    event.accountId,
                    event.requestSource
                )

                is CreateAccountEvent.Dismiss -> onBackClick()
            }
        }
    }
    if (state.shouldShowNoMnemonicError) {
        NoMnemonicAlertDialog {
            viewModel.dismissNoMnemonicError()
        }
    }
}

@Composable
fun CreateAccountContent(
    onAccountNameChange: (String) -> Unit,
    onAccountCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier,
    state: CreateAccountViewModel.CreateAccountUiState,
    uiMessage: UiMessage? = null,
    onUiMessageShown: () -> Unit = {}
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onUiMessageShown
    )
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                backIconType = if (state.isFirstAccount) BackIconType.Back else BackIconType.Close,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = {
                    keyboardController?.hide()
                    onAccountCreateClick()
                },
                text = stringResource(id = R.string.createAccount_nameNewAccount_continue),
                isLoading = state.isCreatingAccount,
                enabled = state.isSubmitEnabled,
                insets = if (isKeyboardVisible()) WindowInsets.ime else WindowInsets.navigationBars
            )
        },
        containerColor = RadixTheme.colors.background,
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge)
                .padding(top = RadixTheme.dimensions.paddingDefault)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.isFirstAccount) {
                    stringResource(id = R.string.createAccount_titleFirst)
                } else {
                    stringResource(id = R.string.createAccount_titleNotFirst)
                },
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                text = stringResource(id = R.string.createAccount_nameNewAccount_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.createAccount_nameNewAccount_explanation),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = onAccountNameChange,
                value = state.accountName,
                error = if (state.isAccountNameErrorVisible) {
                    stringResource(id = R.string.error_accountLabel_tooLong)
                } else {
                    null
                },
                hint = stringResource(id = R.string.createAccount_nameNewAccount_placeholder),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun CreateAccountContentPreview() {
    RadixWalletPreviewTheme {
        CreateAccountContent(
            onAccountNameChange = {},
            onAccountCreateClick = {},
            onBackClick = {},
            modifier = Modifier,
            state = CreateAccountViewModel.CreateAccountUiState(
                accountName = "Main"
            )
        )
    }
}
