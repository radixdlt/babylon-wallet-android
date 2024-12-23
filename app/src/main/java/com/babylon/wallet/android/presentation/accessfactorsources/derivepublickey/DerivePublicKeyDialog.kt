package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickey

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.accessfactorsources.composables.RoundLedgerItem
import com.babylon.wallet.android.presentation.accessfactorsources.derivepublickey.DerivePublicKeyViewModel.DerivePublicKeyUiState
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.sargon.sample

@Composable
fun DerivePublicKeyDialog(
    modifier: Modifier = Modifier,
    viewModel: DerivePublicKeyViewModel,
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
                is DerivePublicKeyViewModel.Event.RequestBiometricPrompt -> {
                    context.biometricAuthenticate { biometricAuthenticationResult ->
                        when (biometricAuthenticationResult) {
                            BiometricAuthenticationResult.Succeeded -> viewModel.biometricAuthenticationCompleted()
                            else -> {
                                /* do nothing */
                            }
                        }
                    }
                }

                DerivePublicKeyViewModel.Event.AccessingFactorSourceCompleted -> onDismiss()
                DerivePublicKeyViewModel.Event.UserDismissed -> onDismiss()
            }
        }
    }

    state.contentType?.let { contentType ->
        DerivePublicKeyBottomSheetContent(
            modifier = modifier,
            contentType = contentType,
            onDismiss = viewModel::onUserDismiss,
            onRetryClick = viewModel::onRetryClick
        )
    }
}

@Composable
private fun DerivePublicKeyBottomSheetContent(
    modifier: Modifier = Modifier,
    contentType: DerivePublicKeyUiState.ContentType,
    onDismiss: () -> Unit,
    onRetryClick: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier,
        onDismiss = onDismiss,
        heightFraction = 0.7f,
        centerContent = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                .background(RadixTheme.colors.defaultBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.size(80.dp),
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_security_key
                ),
                contentDescription = null,
                tint = RadixTheme.colors.gray3
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            val title = when (contentType) {
                DerivePublicKeyUiState.ContentType.ForDeviceAccount,
                is DerivePublicKeyUiState.ContentType.ForLedgerAccount -> {
                    stringResource(id = R.string.factorSourceActions_createAccount_title)
                }

                DerivePublicKeyUiState.ContentType.ForPersona -> {
                    stringResource(id = R.string.factorSourceActions_createPersona_title)
                }
            }
            Text(
                style = RadixTheme.typography.title,
                text = title
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            when (contentType) {
                DerivePublicKeyUiState.ContentType.ForPersona,
                DerivePublicKeyUiState.ContentType.ForDeviceAccount -> {
                    Text(
                        style = RadixTheme.typography.body1Regular,
                        text = stringResource(id = R.string.factorSourceActions_device_messageSignature),
                        textAlign = TextAlign.Center
                    )
                }

                is DerivePublicKeyUiState.ContentType.ForLedgerAccount -> {
                    Text(
                        style = RadixTheme.typography.body1Regular,
                        text = stringResource(id = R.string.factorSourceActions_ledger_message)
                            .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    RoundLedgerItem(ledgerName = contentType.selectedLedgerDevice.value.hint.label)
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            RadixTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.common_retry),
                onClick = onRetryClick
            )
        }
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
private fun DerivePublicKeyPreview(
    @PreviewParameter(DerivePublicKeyPreviewParameterProvider::class) param: DerivePublicKeyUiState.ContentType
) {
    RadixWalletTheme {
        DerivePublicKeyBottomSheetContent(
            contentType = param,
            onDismiss = {},
            onRetryClick = {}
        )
    }
}

@UsesSampleValues
class DerivePublicKeyPreviewParameterProvider : PreviewParameterProvider<DerivePublicKeyUiState.ContentType> {

    override val values: Sequence<DerivePublicKeyUiState.ContentType>
        get() = sequenceOf(
            DerivePublicKeyUiState.ContentType.ForDeviceAccount,
            DerivePublicKeyUiState.ContentType.ForLedgerAccount(selectedLedgerDevice = FactorSource.Ledger.sample()),
            DerivePublicKeyUiState.ContentType.ForPersona
        )
}
