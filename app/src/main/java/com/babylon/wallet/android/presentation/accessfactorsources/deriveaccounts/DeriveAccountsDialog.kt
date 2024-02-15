package com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts.DeriveAccountsViewModel.DeriveAccountsUiState.ShowContentForFactorSource
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.formattedSpans
import rdx.works.profile.domain.TestData

@Composable
fun DeriveAccountsDialog(
    modifier: Modifier = Modifier,
    viewModel: DeriveAccountsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                DeriveAccountsViewModel.Event.RequestBiometricPrompt -> {
                    context.biometricAuthenticate { isAuthenticated ->
                        viewModel.biometricAuthenticationCompleted(isAuthenticated)
                        if (isAuthenticated.not()) {
                            onDismiss()
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
        isDerivingAccountsInProgress = state.isDerivingAccountsInProgress,
        onDismiss = viewModel::onUserDismissed
    )
}

@Composable
private fun DeriveAccountsBottomSheetContent(
    modifier: Modifier = Modifier,
    showContentForFactorSource: ShowContentForFactorSource,
    isDerivingAccountsInProgress: Boolean,
    onDismiss: () -> Unit
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
                text = stringResource(id = R.string.accountRecoveryScan_derivingAccounts)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            when (showContentForFactorSource) {
                ShowContentForFactorSource.Device -> {
                    Text(
                        style = RadixTheme.typography.body1Regular,
                        text = stringResource(id = R.string.derivePublicKeys_subtitleDevice)
                    )
                }
                is ShowContentForFactorSource.Ledger -> {
                    Text(
                        style = RadixTheme.typography.body1Regular,
                        text = stringResource(id = R.string.derivePublicKeys_subtitleLedger)
                            .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold))
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
                    RoundLedgerItem(ledgerName = showContentForFactorSource.selectedLedgerDevice.hint.name)
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            if (isDerivingAccountsInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = RadixTheme.colors.gray1
                )
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun RoundLedgerItem(ledgerName: String) {
    Row(
        modifier = Modifier
            .background(RadixTheme.colors.gray5, RadixTheme.shapes.circle)
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.ic_security_key
            ),
            contentDescription = null,
            tint = RadixTheme.colors.gray3
        )
        Text(
            text = ledgerName,
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1
        )
    }
}

@Preview(showBackground = false)
@Composable
fun DeriveAccountsDeviceDialogPreview() {
    RadixWalletTheme {
        DeriveAccountsBottomSheetContent(
            showContentForFactorSource = ShowContentForFactorSource.Device,
            isDerivingAccountsInProgress = false,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = false)
@Composable
fun DeriveAccountsLedgerDialogPreview() {
    RadixWalletTheme {
        DeriveAccountsBottomSheetContent(
            showContentForFactorSource = ShowContentForFactorSource.Ledger(TestData.ledgerFactorSource),
            isDerivingAccountsInProgress = false,
            onDismiss = {}
        )
    }
}
