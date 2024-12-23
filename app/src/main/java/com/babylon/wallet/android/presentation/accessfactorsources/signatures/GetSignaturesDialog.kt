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
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput.ToSign.Purpose
import com.babylon.wallet.android.presentation.accessfactorsources.composables.RoundLedgerItem
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.samples.sample

@Composable
fun GetSignaturesDialog(
    modifier: Modifier = Modifier,
    viewModel: GetSignaturesViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    BackHandler {
        viewModel.onDismiss()
    }

    state.errorMessage?.let { errorMessage ->
        BasicPromptAlertDialog(
            finish = { viewModel.onMessageShown() },
            messageText = errorMessage.getMessage(),
            dismissText = null
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                GetSignaturesViewModel.Event.Completed -> onDismiss()
            }
        }
    }

    GetSignaturesBottomSheetContent(
        modifier = modifier,
        state = state,
        onDismiss = viewModel::onDismiss,
        onRetryClick = viewModel::onRetry
    )
}

@Composable
private fun GetSignaturesBottomSheetContent(
    modifier: Modifier = Modifier,
    state: GetSignaturesViewModel.State,
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
                text = when (state.signPurpose) {
                    Purpose.TransactionIntents,
                    Purpose.SubIntents -> stringResource(id = R.string.factorSourceActions_signature_title)
                    Purpose.AuthIntents -> stringResource(id = R.string.factorSourceActions_proveOwnership_title)
                }
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            when (val factorSourcesToSign = state.factorSourcesToSign) {
                is GetSignaturesViewModel.State.FactorSourcesToSign.Resolving -> {}
                is GetSignaturesViewModel.State.FactorSourcesToSign.Poly -> {
                    if (factorSourcesToSign.kind == FactorSourceKind.DEVICE) {
                        // This will change in the near future, all device factor sources will be listed.
                        DeviceContent(signPurpose = state.signPurpose)
                    }
                }
                is GetSignaturesViewModel.State.FactorSourcesToSign.Mono -> {
                    when (factorSourcesToSign.factorSource) {
                        is FactorSource.Ledger -> LedgerContent(
                            signPurpose = state.signPurpose,
                            factorSource = factorSourcesToSign.factorSource
                        )
                        is FactorSource.Device -> DeviceContent(signPurpose = state.signPurpose)
                        else -> {
                            // Not yet handled
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            RadixTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.common_retry),
                enabled = !state.isSigningInProgress,
                onClick = onRetryClick
            )
        }
    }
}

@Composable
private fun DeviceContent(
    modifier: Modifier = Modifier,
    signPurpose: Purpose
) {
    Text(
        modifier = modifier,
        style = RadixTheme.typography.body1Regular,
        textAlign = TextAlign.Center,
        text = when (signPurpose) {
            Purpose.TransactionIntents,
            Purpose.SubIntents -> stringResource(id = R.string.factorSourceActions_device_messageSignature)
            Purpose.AuthIntents -> stringResource(id = R.string.factorSourceActions_device_message)
        }
    )
}

@Composable
private fun LedgerContent(
    modifier: Modifier = Modifier,
    signPurpose: Purpose,
    factorSource: FactorSource.Ledger
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val stringRes = when (signPurpose) {
            Purpose.TransactionIntents,
            Purpose.SubIntents -> stringResource(id = R.string.factorSourceActions_ledger_messageSignature)
            Purpose.AuthIntents -> stringResource(id = R.string.factorSourceActions_ledger_message)
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringRes.formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        RoundLedgerItem(
            ledgerName = factorSource.value.hint.label
        )
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
fun GetSignaturesForSigningTransactionWithPolyDevicePreview() {
    RadixWalletPreviewTheme {
        GetSignaturesBottomSheetContent(
            state = GetSignaturesViewModel.State(
                signPurpose = Purpose.TransactionIntents,
                factorSourcesToSign = GetSignaturesViewModel.State.FactorSourcesToSign.Poly(
                    kind = FactorSourceKind.DEVICE,
                    factorSources = listOf(
                        DeviceFactorSource.sample().asGeneral(),
                        DeviceFactorSource.sample.other().asGeneral(),
                    )
                )
            ),
            onDismiss = {},
            onRetryClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
fun GetSignaturesForSigningTransactionWithMonoDevicePreview() {
    RadixWalletPreviewTheme {
        GetSignaturesBottomSheetContent(
            state = GetSignaturesViewModel.State(
                signPurpose = Purpose.TransactionIntents,
                factorSourcesToSign = GetSignaturesViewModel.State.FactorSourcesToSign.Mono(
                    factorSource = DeviceFactorSource.sample().asGeneral()
                )
            ),
            onDismiss = {},
            onRetryClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
fun GetSignaturesForSigningTransactionWithMonoLedgerPreview() {
    RadixWalletTheme {
        GetSignaturesBottomSheetContent(
            state = GetSignaturesViewModel.State(
                signPurpose = Purpose.TransactionIntents,
                factorSourcesToSign = GetSignaturesViewModel.State.FactorSourcesToSign.Mono(
                    factorSource = LedgerHardwareWalletFactorSource.sample().asGeneral()
                )
            ),
            onDismiss = {},
            onRetryClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
fun GetSignaturesForProvingOwnershipWithPolyDevicePreview() {
    RadixWalletTheme {
        GetSignaturesBottomSheetContent(
            state = GetSignaturesViewModel.State(
                signPurpose = Purpose.AuthIntents,
                factorSourcesToSign = GetSignaturesViewModel.State.FactorSourcesToSign.Poly(
                    kind = FactorSourceKind.DEVICE,
                    factorSources = listOf(
                        DeviceFactorSource.sample().asGeneral(),
                        DeviceFactorSource.sample.other().asGeneral()
                    )
                )
            ),
            onDismiss = {},
            onRetryClick = {}
        )
    }
}
