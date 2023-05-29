package com.babylon.wallet.android.presentation.createaccount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSwitch
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.utils.biometricAuthenticate

@Composable
fun CreateAccountScreen(
    viewModel: CreateAccountViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    cancelable: Boolean,
    onContinueClick: (
        accountId: String,
        requestSource: CreateAccountRequestSource?,
    ) -> Unit = { _: String, _: CreateAccountRequestSource? -> },
    onAddLedgerDevice: () -> Unit,
    onCloseApp: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backHandler = {
        if (state.firstTime) {
            onCloseApp()
        } else {
            onBackClick()
        }
    }
    BackHandler(onBack = backHandler)
    if (state.loading) {
        FullscreenCircularProgressContent()
    } else {
        val accountName = viewModel.accountName.collectAsStateWithLifecycle().value
        val buttonEnabled = viewModel.buttonEnabled.collectAsStateWithLifecycle().value

        CreateAccountContent(
            onAccountNameChange = viewModel::onAccountNameChange,
            onAccountCreateClick = viewModel::onAccountCreateClick,
            accountName = accountName,
            buttonEnabled = buttonEnabled,
            cancelable = cancelable,
            onBackClick = backHandler,
            modifier = modifier,
            isDeviceSecure = state.isDeviceSecure,
            firstTime = state.firstTime,
            useLedgerSelected = state.useLedgerSelected,
            onUseLedgerSelectionChanged = viewModel::onUseLedgerSelectionChanged
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is CreateAccountEvent.Complete -> onContinueClick(
                    event.accountId,
                    event.requestSource
                )
                CreateAccountEvent.AddLedgerDevice -> onAddLedgerDevice()
            }
        }
    }
}

@Composable
fun CreateAccountContent(
    onAccountNameChange: (String) -> Unit,
    onAccountCreateClick: () -> Unit,
    accountName: String,
    buttonEnabled: Boolean,
    onBackClick: () -> Unit,
    cancelable: Boolean,
    isDeviceSecure: Boolean,
    modifier: Modifier,
    firstTime: Boolean,
    useLedgerSelected: Boolean,
    onUseLedgerSelectionChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
    ) {
        var showNotSecuredDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        if (cancelable) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                    tint = RadixTheme.colors.gray1,
                    contentDescription = "navigate back"
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingXLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (firstTime) {
                    stringResource(id = R.string.create_first_account)
                } else {
                    stringResource(id = R.string.homePage_createNewAccount)
                },
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(id = R.string.account_creation_text),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(30.dp))
            UseLedgerSwitch(
                useLedgerSelected = useLedgerSelected,
                onUseLedgerSelectionChanged = onUseLedgerSelectionChanged,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(30.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                RadixTextField(
                    modifier = Modifier.fillMaxWidth(),
                    onValueChanged = onAccountNameChange,
                    value = accountName,
                    hint = stringResource(id = R.string.hint_account_name),
                    singleLine = true
                )
                Text(
                    text = stringResource(id = R.string.this_can_be_changed_any_time),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            Spacer(Modifier.weight(1f))
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                onClick = {
                    when {
                        useLedgerSelected -> onAccountCreateClick()
                        isDeviceSecure -> context.biometricAuthenticate { authenticatedSuccessfully ->
                            if (authenticatedSuccessfully) {
                                onAccountCreateClick()
                            }
                        }
                        else -> showNotSecuredDialog = true
                    }
                },
                enabled = buttonEnabled,
                text = stringResource(id = R.string.create_account),
                throttleClicks = true
            )
        }

        if (showNotSecuredDialog) {
            NotSecureAlertDialog(finish = {
                showNotSecuredDialog = false
                if (it) {
                    onAccountCreateClick()
                }
            })
        }
    }
}

@Composable
private fun UseLedgerSwitch(useLedgerSelected: Boolean, onUseLedgerSelectionChanged: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.create_with_ledger_hardware_wallet),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            Text(
                text = stringResource(id = R.string.requires_you_to_sign_transactions),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
        }
        RadixSwitch(checked = useLedgerSelected, onCheckedChange = onUseLedgerSelectionChanged)
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
            buttonEnabled = false,
            onBackClick = {},
            cancelable = true,
            isDeviceSecure = true,
            modifier = Modifier,
            firstTime = false,
            useLedgerSelected = false,
            onUseLedgerSelectionChanged = {}
        )
    }
}
