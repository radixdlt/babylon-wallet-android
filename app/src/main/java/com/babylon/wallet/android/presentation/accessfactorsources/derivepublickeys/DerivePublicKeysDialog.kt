package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickeys

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.babylon.wallet.android.presentation.accessfactorsources.derivepublickeys.DerivePublicKeysViewModel.State.Content
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.DerivationPurpose
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.sargon.sample

@Composable
fun DerivePublicKeysDialog(
    modifier: Modifier = Modifier,
    viewModel: DerivePublicKeysViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    BackHandler {
        viewModel.onUserDismiss()
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                DerivePublicKeysViewModel.Event.Dismiss -> onDismiss()
            }
        }
    }

    DerivePublicKeyBottomSheetContent(
        modifier = modifier,
        state = state,
        onDismiss = viewModel::onUserDismiss,
        onRetryClick = viewModel::onRetryClick
    )
}

@Composable
private fun DerivePublicKeyBottomSheetContent(
    modifier: Modifier = Modifier,
    state: DerivePublicKeysViewModel.State,
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

            when (val content = state.content) {
                Content.Resolving -> {
                    Box(
                        modifier = Modifier.height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = RadixTheme.colors.gray1
                        )
                    }
                }
                is Content.Resolved -> {
                    Text(
                        style = RadixTheme.typography.title,
                        text = when (content.purpose) {
                            DerivationPurpose.CREATING_NEW_ACCOUNT -> stringResource(R.string.factorSourceActions_createAccount_title)
                            DerivationPurpose.CREATING_NEW_PERSONA -> stringResource(R.string.factorSourceActions_createPersona_title)
                            DerivationPurpose.SECURIFYING_ACCOUNT -> "TBD"
                            DerivationPurpose.SECURIFYING_PERSONA -> "TBD"
                            DerivationPurpose.PRE_DERIVING_KEYS -> stringResource(R.string.factorSourceActions_deriveAccounts_title)
                        }
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                    when (val factorSource = content.factorSource) {
                        is FactorSource.Device -> {
                            Text(
                                style = RadixTheme.typography.body1Regular,
                                text = stringResource(id = R.string.factorSourceActions_device_messageSignature),
                                textAlign = TextAlign.Center
                            )
                        }
                        is FactorSource.Ledger -> {
                            Text(
                                style = RadixTheme.typography.body1Regular,
                                text = stringResource(id = R.string.factorSourceActions_ledger_message)
                                    .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                            RoundLedgerItem(ledgerName = factorSource.value.hint.label)
                        }
                        else -> {}
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
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
private fun DerivePublicKeyPreview(
    @PreviewParameter(DerivePublicKeysPreviewParameterProvider::class) param: DerivePublicKeysViewModel.State
) {
    RadixWalletTheme {
        DerivePublicKeyBottomSheetContent(
            state = param,
            onDismiss = {},
            onRetryClick = {}
        )
    }
}

@UsesSampleValues
class DerivePublicKeysPreviewParameterProvider : PreviewParameterProvider<DerivePublicKeysViewModel.State> {

    override val values: Sequence<DerivePublicKeysViewModel.State>
        get() = sequenceOf(
            DerivePublicKeysViewModel.State(Content.Resolving),
            DerivePublicKeysViewModel.State(
                Content.Resolved(
                    purpose = DerivationPurpose.CREATING_NEW_ACCOUNT,
                    factorSource = FactorSource.Device.sample()
                )
            ),
            DerivePublicKeysViewModel.State(
                Content.Resolved(
                    purpose = DerivationPurpose.CREATING_NEW_PERSONA,
                    factorSource = FactorSource.Ledger.sample()
                )
            ),
        )
}
