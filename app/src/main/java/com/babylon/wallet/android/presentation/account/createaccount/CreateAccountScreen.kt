package com.babylon.wallet.android.presentation.account.createaccount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSwitch
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.NoMnemonicAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.radixdlt.sargon.AccountAddress

@Composable
fun CreateAccountScreen(
    viewModel: CreateAccountViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueClick: (
        accountId: AccountAddress,
        requestSource: CreateAccountRequestSource?,
    ) -> Unit = { _: AccountAddress, _: CreateAccountRequestSource? -> },
    onAddLedgerDevice: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = viewModel::onBackClick)
    if (state.loading) {
        FullscreenCircularProgressContent()
    } else {
        val accountName by viewModel.accountName.collectAsStateWithLifecycle()
        val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
        val isAccountNameLengthMoreThanTheMax by viewModel.isAccountNameLengthMoreThanTheMax.collectAsStateWithLifecycle()

        val context = LocalContext.current
        CreateAccountContent(
            onAccountNameChange = viewModel::onAccountNameChange,
            onAccountCreateClick = {
                viewModel.onAccountCreateClick(
                    isWithLedger = it,
                    biometricAuthProvider = { context.biometricAuthenticateSuspend() }
                )
            },
            accountName = accountName,
            isAccountNameLengthMoreThanTheMaximum = isAccountNameLengthMoreThanTheMax,
            buttonEnabled = buttonEnabled,
            onBackClick = viewModel::onBackClick,
            modifier = modifier,
            firstTime = state.firstTime,
            isWithLedger = state.isWithLedger,
            onUseLedgerSelectionChanged = viewModel::onUseLedgerSelectionChanged,
            uiMessage = state.uiMessage,
            onUiMessageShown = viewModel::onUiMessageShown
        )
    }
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is CreateAccountEvent.Complete -> onContinueClick(
                    event.accountId,
                    event.requestSource
                )

                is CreateAccountEvent.AddLedgerDevice -> onAddLedgerDevice()
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
    onAccountCreateClick: (Boolean) -> Unit,
    accountName: String,
    isAccountNameLengthMoreThanTheMaximum: Boolean,
    buttonEnabled: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier,
    firstTime: Boolean,
    isWithLedger: Boolean,
    onUseLedgerSelectionChanged: (Boolean) -> Unit,
    uiMessage: UiMessage? = null,
    onUiMessageShown: () -> Unit = {}
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onUiMessageShown
    )

    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                backIconType = BackIconType.Close,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            RadixPrimaryButton(
                text = stringResource(id = R.string.createAccount_nameNewAccount_continue),
                onClick = {
                    onAccountCreateClick(isWithLedger)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    )
                    .navigationBarsPadding(),
                enabled = buttonEnabled,
                throttleClicks = true
            )
        },
        containerColor = RadixTheme.colors.defaultBackground,
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
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(top = RadixTheme.dimensions.paddingDefault)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (firstTime) {
                    stringResource(id = R.string.createAccount_titleFirst)
                } else {
                    stringResource(id = R.string.createAccount_titleNotFirst)
                },
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                text = stringResource(id = R.string.createAccount_nameNewAccount_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.createAccount_nameNewAccount_explanation),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray2,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = onAccountNameChange,
                value = accountName,
                error = if (isAccountNameLengthMoreThanTheMaximum) {
                    stringResource(id = R.string.error_accountLabel_tooLong)
                } else {
                    null
                },
                hint = stringResource(id = R.string.createAccount_nameNewAccount_placeholder),
                hintColor = RadixTheme.colors.gray2,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            CreateWithLedgerSwitch(
                isChecked = isWithLedger,
                onUseLedgerSelectionChanged = onUseLedgerSelectionChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CreateWithLedgerSwitch(
    isChecked: Boolean,
    onUseLedgerSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.createEntity_nameNewEntity_ledgerTitle),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )
            Text(
                text = stringResource(id = R.string.createEntity_nameNewEntity_ledgerSubtitle),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
        RadixSwitch(checked = isChecked, onCheckedChange = onUseLedgerSelectionChanged)
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun CreateAccountContentPreview() {
    RadixWalletTheme {
        CreateAccountContent(
            onAccountNameChange = {},
            onAccountCreateClick = {},
            accountName = "Name",
            isAccountNameLengthMoreThanTheMaximum = false,
            buttonEnabled = false,
            onBackClick = {},
            modifier = Modifier,
            firstTime = false,
            isWithLedger = false,
            onUseLedgerSelectionChanged = {}
        )
    }
}
