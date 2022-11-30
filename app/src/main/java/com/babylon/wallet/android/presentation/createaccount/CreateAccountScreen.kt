package com.babylon.wallet.android.presentation.createaccount

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun CreateAccountScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueClick: (accountId: String, accountName: String) -> Unit = { _: String, _: String -> },
) {
    var buttonEnabled by rememberSaveable { mutableStateOf(false) }
    var accountName by rememberSaveable { mutableStateOf("") }
    val maxLength = 20

    Column(
        modifier = modifier
            .systemBarsPadding().background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                tint = RadixTheme.colors.gray1,
                contentDescription = "navigate back"
            )
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
                    onValueChanged = {
                        buttonEnabled = it.isNotEmpty()
                        accountName = it.take(maxLength)
                    },
                    value = accountName,
                    hint = stringResource(id = R.string.account_name)
                )
                Text(
                    text = stringResource(id = R.string.this_can_be_changed_any_time),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            Spacer(Modifier.weight(1f))
            RadixPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                // TODO Im gonna revist this to handle that from viewmodel nicely.
                //  In the meantime, we dont have account generation so i put hardcoded stuff here
                onClick = { onContinueClick("di20ejdnd2e20e2", accountName) },
                enabled = buttonEnabled,
                text = stringResource(id = R.string.continue_button_title)
            )
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun CreateAccountPreview() {
    BabylonWalletTheme {
        CreateAccountScreen(
            onBackClick = {},
            onContinueClick = { _, _ -> }
        )
    }
}
