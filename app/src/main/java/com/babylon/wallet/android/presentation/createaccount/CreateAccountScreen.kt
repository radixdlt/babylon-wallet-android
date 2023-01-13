package com.babylon.wallet.android.presentation.createaccount

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun CreateAccountScreen(
    viewModel: CreateAccountViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    cancelable: Boolean,
    onContinueClick: (
        accountId: String,
        requestSource: CreateAccountRequestSource?
    ) -> Unit = { _: String, _: CreateAccountRequestSource? -> }
) {
    if (viewModel.state.loading) {
        FullscreenCircularProgressContent()
    } else {
        val state = viewModel.state
        val accountName = viewModel.accountName.collectAsStateWithLifecycle().value
        val buttonEnabled = viewModel.buttonEnabled.collectAsStateWithLifecycle().value

        CreateAccountContent(
            onAccountNameChange = viewModel::onAccountNameChange,
            onAccountCreateClick = viewModel::onAccountCreateClick,
            accountName = accountName,
            buttonEnabled = buttonEnabled,
            cancelable = cancelable,
            onBackClick = onBackClick,
            modifier = modifier,
            isDeviceSecure = state.isDeviceSecure
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is CreateAccountEvent.Complete -> onContinueClick(
                    event.accountId,
                    event.requestSource
                )
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
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
    ) {
        val showNotSecuredDialog = remember { mutableStateOf(false) }
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
            Image(
                painter = painterResource(R.drawable.img_account_creation),
                contentDescription = "account_creation_image"
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                text = stringResource(id = R.string.create_new_account),
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(id = R.string.account_creation_text),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(30.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                RadixTextField(
                    modifier = Modifier.fillMaxWidth(),
                    onValueChanged = onAccountNameChange,
                    value = accountName,
                    hint = stringResource(id = R.string.account_name),
                    singleLine = true
                )
                Text(
                    text = stringResource(id = R.string.this_can_be_changed_any_time),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            Spacer(Modifier.weight(1f))
            val context = LocalContext.current
            RadixPrimaryButton(
                modifier = Modifier.fillMaxWidth().imePadding(),
                onClick = {
                    if (isDeviceSecure) {
                        context.findFragmentActivity()?.let { activity ->
                            activity.biometricAuthenticate(true) { authenticatedSuccessfully ->
                                if (authenticatedSuccessfully) {
                                    onAccountCreateClick()
                                }
                            }
                        }
                    } else {
                        showNotSecuredDialog.value = true
                    }
                },
                enabled = buttonEnabled,
                text = stringResource(id = R.string.continue_button_title)
            )
        }
        NotSecureAlertDialog(show = showNotSecuredDialog.value, finish = {
            showNotSecuredDialog.value = false
            if (it) {
                onAccountCreateClick()
            }
        })
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
            modifier = Modifier
        )
    }
}
