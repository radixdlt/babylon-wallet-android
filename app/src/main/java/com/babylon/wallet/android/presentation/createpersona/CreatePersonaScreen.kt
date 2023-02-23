package com.babylon.wallet.android.presentation.createpersona

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
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity

@Composable
fun CreatePersonaScreen(
    viewModel: CreatePersonaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueClick: (
        personaId: String
    ) -> Unit = { _: String -> },
) {
    if (viewModel.state.loading) {
        FullscreenCircularProgressContent()
    } else {
        val state = viewModel.state

        CreatePersonaContent(
            onPersonaNameChange = viewModel::onPersonaNameChange,
            onPersonaCreateClick = viewModel::onPersonaCreateClick,
            personaName = state.personaName,
            buttonEnabled = state.buttonEnabled,
            onBackClick = onBackClick,
            modifier = modifier,
            isDeviceSecure = state.isDeviceSecure
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is CreatePersonaEvent.Complete -> onContinueClick(
                    event.personaId
                )
            }
        }
    }
}

@Composable
fun CreatePersonaContent(
    onPersonaNameChange: (String) -> Unit,
    onPersonaCreateClick: () -> Unit,
    personaName: String,
    buttonEnabled: Boolean,
    onBackClick: () -> Unit,
    isDeviceSecure: Boolean,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
    ) {
        var showNotSecuredDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        IconButton(onClick = onBackClick) {
            Icon(
                painterResource(id = R.drawable.ic_close),
                tint = RadixTheme.colors.gray1,
                contentDescription = "navigate back"
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.create_a_persona),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.persona_creation_text),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(30.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                RadixTextField(
                    modifier = Modifier.fillMaxWidth(),
                    onValueChanged = onPersonaNameChange,
                    value = personaName,
                    hint = stringResource(id = com.babylon.wallet.android.R.string.hint_persona_name),
                    singleLine = true
                )
                Text(
                    text = stringResource(id = com.babylon.wallet.android.R.string.this_can_be_changed_any_time),
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
                    if (isDeviceSecure) {
                        context.findFragmentActivity()?.let { activity ->
                            activity.biometricAuthenticate(true) { authenticatedSuccessfully ->
                                if (authenticatedSuccessfully) {
                                    onPersonaCreateClick()
                                }
                            }
                        }
                    } else {
                        showNotSecuredDialog = true
                    }
                },
                enabled = buttonEnabled,
                text = stringResource(id = com.babylon.wallet.android.R.string.continue_button_title)
            )
        }
        if (showNotSecuredDialog) {
            NotSecureAlertDialog(finish = {
                showNotSecuredDialog = false
                if (it) {
                    onPersonaCreateClick()
                }
            })
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun CreateAccountContentPreview() {
    RadixWalletTheme {
        CreatePersonaContent(
            onPersonaNameChange = {},
            onPersonaCreateClick = {},
            personaName = "Name",
            buttonEnabled = false,
            onBackClick = {},
            isDeviceSecure = true,
            modifier = Modifier
        )
    }
}
