package com.babylon.wallet.android.presentation.accessfactorsources

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesViewModel.AccessFactorSourcesUiState
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.LedgerListItem
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun AccessFactorSourcesDialog(
    modifier: Modifier = Modifier,
    viewModel: AccessFactorSourcesViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                AccessFactorSourcesViewModel.Event.RequestBiometricPrompt -> {
                    context.biometricAuthenticate { isAuthenticated ->
                        viewModel.biometricAuthenticationCompleted(isAuthenticated)
                        if (isAuthenticated.not()) {
                            onDismiss()
                        }
                    }
                }
            }
        }
    }

    AccessFactorSourcesBottomSheetContent(
        modifier = modifier,
        isAccessingFactorSourceInProgress = state.isAccessingFactorSourceInProgress,
        isAccessingFactorSourceCompleted = state.isAccessingFactorSourceCompleted,
        showContentForFactorSource = state.showContentFor,
        onDismiss = onDismiss
    )
}

@Composable
private fun AccessFactorSourcesBottomSheetContent(
    modifier: Modifier = Modifier,
    isAccessingFactorSourceInProgress: Boolean,
    isAccessingFactorSourceCompleted: Boolean,
    showContentForFactorSource: AccessFactorSourcesUiState.ShowContentFor,
    onDismiss: () -> Unit
) {
    if (isAccessingFactorSourceCompleted) {
        onDismiss()
    }

    BottomSheetDialogWrapper(
        modifier = modifier,
        onDismiss = {
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                .background(RadixTheme.colors.defaultBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_create_account),
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                style = RadixTheme.typography.title,
                text = stringResource(id = R.string.derivePublicKeys_titleCreateAccount)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            when (showContentForFactorSource) {
                AccessFactorSourcesUiState.ShowContentFor.Device -> {
                    Text(
                        style = RadixTheme.typography.body1Regular,
                        text = stringResource(id = R.string.derivePublicKeys_subtitleDevice)
                    )
                }

                is AccessFactorSourcesUiState.ShowContentFor.Ledger -> {
                    Text(
                        style = RadixTheme.typography.body1Regular,
                        text = stringResource(id = R.string.derivePublicKeys_subtitleLedger)
                            .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold))
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    LedgerListItem(
                        ledgerFactorSource = showContentForFactorSource.selectedLedgerDevice,
                        modifier = Modifier
                            .shadow(elevation = 4.dp, shape = RadixTheme.shapes.roundedRectSmall)
                            .fillMaxWidth()
                            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectSmall)
                            .padding(RadixTheme.dimensions.paddingLarge),

                    )
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            if (isAccessingFactorSourceInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = RadixTheme.colors.gray1
                )
            }
            Spacer(Modifier.height(70.dp))
        }
    }
}
