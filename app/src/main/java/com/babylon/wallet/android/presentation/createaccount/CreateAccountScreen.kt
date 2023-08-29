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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
    onAddLedgerDevice: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = viewModel::onBackClick)
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
            onBackClick = viewModel::onBackClick,
            modifier = modifier,
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

                is CreateAccountEvent.AddLedgerDevice -> onAddLedgerDevice()
                is CreateAccountEvent.Dismiss -> onBackClick()
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
    modifier: Modifier,
    firstTime: Boolean,
    useLedgerSelected: Boolean,
    onUseLedgerSelectionChanged: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
    ) {
        val context = LocalContext.current

        if (cancelable) {
            IconButton(
                modifier = Modifier
                    .padding(
                        start = RadixTheme.dimensions.paddingDefault,
                        top = RadixTheme.dimensions.paddingDefault
                    ),
                onClick = onBackClick
            ) {
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
            Spacer(modifier = Modifier.height(30.dp))
            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = onAccountNameChange,
                value = accountName,
                hint = stringResource(id = R.string.createAccount_nameNewAccount_placeholder),
                hintColor = RadixTheme.colors.gray2,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            UseLedgerSwitch(
                useLedgerSelected = useLedgerSelected,
                onUseLedgerSelectionChanged = onUseLedgerSelectionChanged,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.weight(1f))
            RadixPrimaryButton(
                text = stringResource(id = R.string.createAccount_nameNewAccount_continue),
                onClick = {
                    when {
                        useLedgerSelected -> onAccountCreateClick()
                        else -> context.biometricAuthenticate { authenticatedSuccessfully ->
                            if (authenticatedSuccessfully) {
                                onAccountCreateClick()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RadixTheme.dimensions.paddingLarge)
                    .imePadding(),
                enabled = buttonEnabled,
                throttleClicks = true
            )
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
            modifier = Modifier,
            firstTime = false,
            useLedgerSelected = false,
            onUseLedgerSelectionChanged = {}
        )
    }
}
