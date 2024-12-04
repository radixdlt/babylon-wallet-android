package com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts

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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.accessfactorsources.composables.RoundLedgerItem
import com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts.DeriveAccountsViewModel.DeriveAccountsUiState.ShowContentForFactorSource
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.sargon.sample

@Composable
fun DeriveAccountsDialog(
    modifier: Modifier = Modifier,
    viewModel: DeriveAccountsViewModel,
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
                DeriveAccountsViewModel.Event.RequestBiometricPrompt -> {
                    context.biometricAuthenticate { biometricAuthenticationResult ->
                        when (biometricAuthenticationResult) {
                            BiometricAuthenticationResult.Succeeded -> viewModel.biometricAuthenticationCompleted()
                            else -> {
                                /* do nothing */
                            }
                        }
                    }
                }

                DeriveAccountsViewModel.Event.DerivingAccountsCompleted -> onDismiss()
                DeriveAccountsViewModel.Event.UserDismissed -> onDismiss()
            }
        }
    }

    DeriveAccountsBottomSheetContent(
        modifier = modifier,
        showContentForFactorSource = state.showContentForFactorSource,
        onDismiss = viewModel::onUserDismiss,
        onRetryClick = viewModel::onRetryClick
    )
}

@Composable
private fun DeriveAccountsBottomSheetContent(
    modifier: Modifier = Modifier,
    showContentForFactorSource: ShowContentForFactorSource,
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
            Text(
                style = RadixTheme.typography.title,
                text = stringResource(id = R.string.factorSourceActions_deriveAccounts_title)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            when (showContentForFactorSource) {
                ShowContentForFactorSource.Device -> {
                    Text(
                        style = RadixTheme.typography.body1Regular,
                        text = stringResource(id = R.string.factorSourceActions_device_messageSignature)
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    RadixTextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.common_retry),
                        onClick = onRetryClick
                    )
                }

                is ShowContentForFactorSource.Ledger -> {
                    Text(
                        style = RadixTheme.typography.body1Regular,
                        text = stringResource(id = R.string.factorSourceActions_ledger_messageDeriveAccounts)
                            .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    RoundLedgerItem(ledgerName = showContentForFactorSource.selectedLedgerDevice.value.hint.label)
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    RadixTextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.common_retry),
                        onClick = onRetryClick
                    )
                }
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun DeriveAccountsDeviceDialogPreview() {
    RadixWalletTheme {
        DeriveAccountsBottomSheetContent(
            showContentForFactorSource = ShowContentForFactorSource.Device,
            onDismiss = {},
            onRetryClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = false, device = Devices.NEXUS_5)
@Composable
fun DeriveAccountsLedgerDialogPreview() {
    RadixWalletTheme {
        DeriveAccountsBottomSheetContent(
            showContentForFactorSource = ShowContentForFactorSource.Ledger(FactorSource.Ledger.sample()),
            onDismiss = {},
            onRetryClick = {}
        )
    }
}
