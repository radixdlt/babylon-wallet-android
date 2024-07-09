package com.babylon.wallet.android.presentation.accessfactorsources.createpersona

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate

@Composable
fun CreatePersonaDialog(
    modifier: Modifier = Modifier,
    viewModel: CreatePersonaDialogViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    BackHandler {
        viewModel.onUserDismiss()
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                CreatePersonaDialogViewModel.Event.AccessingFactorSourceCompleted -> onDismiss()
                CreatePersonaDialogViewModel.Event.RequestBiometricPrompt -> {
                    context.biometricAuthenticate { biometricAuthenticationResult ->
                        when (biometricAuthenticationResult) {
                            BiometricAuthenticationResult.Succeeded -> viewModel.biometricAuthenticationCompleted()
                            BiometricAuthenticationResult.Error -> viewModel.onBiometricAuthenticationDismiss()
                            BiometricAuthenticationResult.Failed -> {
                                /* do nothing */
                            }
                        }
                    }
                }

                CreatePersonaDialogViewModel.Event.UserDismissed -> onDismiss()
            }
        }
    }

    CreatePersonaDialogContent(
        modifier = modifier,
        shouldShowRetryButton = state.shouldShowRetryButton,
        onDismiss = viewModel::onUserDismiss,
        onRetryClick = viewModel::onRetryClick
    )
}

@Composable
private fun CreatePersonaDialogContent(
    modifier: Modifier = Modifier,
    shouldShowRetryButton: Boolean,
    onDismiss: () -> Unit,
    onRetryClick: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                .background(RadixTheme.colors.defaultBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Icon(
                modifier = Modifier.size(80.dp),
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_security_key
                ),
                contentDescription = null,
                tint = RadixTheme.colors.gray3
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                style = RadixTheme.typography.title,
                text = stringResource(id = R.string.factorSourceActions_createPersona_title)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Text(
                style = RadixTheme.typography.body1Regular,
                text = stringResource(id = R.string.factorSourceActions_device_messageSignature)
            )
            if (shouldShowRetryButton) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
                RadixTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.common_retry),
                    onClick = onRetryClick
                )
            } else {
                Spacer(modifier = Modifier.height(76.dp))
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Preview(showBackground = false)
@Composable
fun CreatePersonaDialogContentPreview() {
    RadixWalletTheme {
        CreatePersonaDialogContent(
            shouldShowRetryButton = false,
            onDismiss = {},
            onRetryClick = {}
        )
    }
}
