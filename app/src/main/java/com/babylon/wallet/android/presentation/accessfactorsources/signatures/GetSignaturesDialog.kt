package com.babylon.wallet.android.presentation.accessfactorsources.signatures

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
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.presentation.accessfactorsources.composables.RoundLedgerItem
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.GetSignaturesViewModel.State
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.sargon.sample

@Composable
fun GetSignaturesDialog(
    modifier: Modifier = Modifier,
    viewModel: GetSignaturesViewModel,
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
                is GetSignaturesViewModel.Event.RequestBiometricToAccessDeviceFactorSources -> {
                    context.biometricAuthenticate { biometricAuthenticationResult ->
                        when (biometricAuthenticationResult) {
                            BiometricAuthenticationResult.Succeeded -> viewModel.collectSignaturesForDeviceFactorSource()
                            BiometricAuthenticationResult.Error -> { /* do nothing */ }
                            BiometricAuthenticationResult.Failed -> { /* do nothing */ }
                        }
                    }
                }

                GetSignaturesViewModel.Event.AccessingFactorSourceCompleted -> onDismiss()
                GetSignaturesViewModel.Event.UserDismissed -> onDismiss()
            }
        }
    }

    GetSignaturesBottomSheetContent(
        modifier = modifier,
        signPurpose = state.signPurpose,
        showContentForFactorSource = state.showContentForFactorSource,
        isRetryButtonEnabled = state.isRetryButtonEnabled,
        onDismiss = viewModel::onUserDismiss,
        onRetryClick = viewModel::onRetryClick
    )
}

@Composable
private fun GetSignaturesBottomSheetContent(
    modifier: Modifier = Modifier,
    signPurpose: SignPurpose,
    showContentForFactorSource: State.ShowContentForFactorSource,
    isRetryButtonEnabled: Boolean,
    onDismiss: () -> Unit,
    onRetryClick: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier,
        heightFraction = 0.7f,
        centerContent = true,
        onDismiss = onDismiss
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
            Text(
                style = RadixTheme.typography.title,
                text = when (signPurpose) {
                    SignPurpose.SignTransaction -> stringResource(id = R.string.factorSourceActions_signature_title)
                    SignPurpose.SignAuth -> stringResource(id = R.string.factorSourceActions_proveOwnership_title)
                }
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            when (showContentForFactorSource) {
                is State.ShowContentForFactorSource.Device -> {
                    Text(
                        style = RadixTheme.typography.body1Regular,
                        textAlign = TextAlign.Center,
                        text = when (signPurpose) {
                            SignPurpose.SignTransaction -> stringResource(id = R.string.factorSourceActions_device_messageSignature)
                            SignPurpose.SignAuth -> stringResource(id = R.string.factorSourceActions_device_message)
                        }
                    )
                }

                is State.ShowContentForFactorSource.Ledger -> {
                    val stringRes = when (signPurpose) {
                        SignPurpose.SignTransaction -> stringResource(id = R.string.factorSourceActions_ledger_messageSignature)
                        SignPurpose.SignAuth -> stringResource(id = R.string.factorSourceActions_ledger_message)
                    }
                    Text(
                        text = stringRes.formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    RoundLedgerItem(ledgerName = showContentForFactorSource.ledgerFactorSource.value.hint.name)
                }

                State.ShowContentForFactorSource.None -> { /* nothing */ }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            RadixTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.common_retry),
                enabled = isRetryButtonEnabled,
                onClick = onRetryClick
            )
        }
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
fun GetSignaturesForSigningTransactionWithDevicePreview() {
    RadixWalletTheme {
        GetSignaturesBottomSheetContent(
            signPurpose = SignPurpose.SignTransaction,
            showContentForFactorSource = State.ShowContentForFactorSource.Device,
            isRetryButtonEnabled = true,
            onDismiss = {},
            onRetryClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
fun GetSignaturesForSigningTransactionWithLedgerPreview() {
    RadixWalletTheme {
        GetSignaturesBottomSheetContent(
            signPurpose = SignPurpose.SignTransaction,
            showContentForFactorSource = State.ShowContentForFactorSource.Ledger(
                ledgerFactorSource = FactorSource.Ledger.sample()
            ),
            isRetryButtonEnabled = true,
            onDismiss = {},
            onRetryClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
fun GetSignaturesForProvingOwnershipWithDevicePreview() {
    RadixWalletTheme {
        GetSignaturesBottomSheetContent(
            signPurpose = SignPurpose.SignAuth,
            showContentForFactorSource = State.ShowContentForFactorSource.Device,
            isRetryButtonEnabled = true,
            onDismiss = {},
            onRetryClick = {}
        )
    }
}
