package com.babylon.wallet.android.presentation.status.signing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.GradientBrand2
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.utils.formattedSpans
import rdx.works.core.HexCoded32Bytes
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.SigningPurpose

@Composable
fun FactorSourceInteractionBottomDialog(
    modifier: Modifier = Modifier,
    onDismissDialogClick: () -> Unit,
    interactionState: InteractionState
) {
    val dismissHandler = {
        onDismissDialogClick()
    }
    BottomSheetDialogWrapper(
        onDismiss = dismissHandler,
        addScrim = true
    ) {
        FactorSourceInteractionBottomDialogContent(
            modifier = modifier,
            interactionState = interactionState
        )
    }
}

@Composable
fun FactorSourceInteractionBottomDialogContent(
    modifier: Modifier = Modifier,
    interactionState: InteractionState?
) {
    when (interactionState) {
        is InteractionState.Ledger.Success,
        is InteractionState.Device.Success -> {
            SignatureSuccessfulContent(modifier = modifier)
        }

        is InteractionState.Ledger.DerivingPublicKey,
        is InteractionState.Ledger.Pending,
        is InteractionState.Device.DerivingAccounts,
        is InteractionState.Device.Pending -> {
            SignatureRequestContent(interactionState, modifier)
        }
        else -> {}
    }
}

@Composable
private fun SignatureRequestContent(
    interactionState: InteractionState?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.defaultBackground)
            .padding(RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Icon(
            modifier = Modifier.size(80.dp),
            painter = painterResource(
                id = R.drawable.ic_security_key
            ),
            contentDescription = null,
            tint = RadixTheme.colors.gray3
        )
        val title = stringResource(
            id = when (interactionState) {
                is InteractionState.Ledger.DerivingPublicKey -> {
                    com.babylon.wallet.android.R.string.createAccount_derivePublicKeys_subtitle
                }

                is InteractionState.Ledger -> {
                    com.babylon.wallet.android.R.string.signing_signatureRequest_title
                }
                is InteractionState.Device.DerivingAccounts -> {
                    com.babylon.wallet.android.R.string.recoveryScan_derivingAccounts
                }

                is InteractionState.Device -> com.babylon.wallet.android.R.string.signing_signatureRequest_title
                else -> com.babylon.wallet.android.R.string.empty
            }
        )
        Text(
            text = title,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        val subtitle = when (interactionState) {
            is InteractionState.Device.Success -> {
                signingPurposeDescription(interactionState.signingPurpose)
            }

            is InteractionState.Device.Pending -> {
                signingPurposeDescription(interactionState.signingPurpose)
            }

            is InteractionState.Ledger.Success,
            is InteractionState.Ledger.Pending -> {
                com.babylon.wallet.android.R.string.signing_signatureRequest_body
            }

            else -> null
        }
        Text(
            text = subtitle?.let {
                stringResource(it).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold))
            } ?: AnnotatedString(""),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        if (interactionState?.usingLedger == true) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Row(
                modifier = Modifier
                    .background(RadixTheme.colors.gray5, RadixTheme.shapes.circle)
                    .padding(RadixTheme.dimensions.paddingDefault),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                Icon(
                    painter = painterResource(
                        id = R.drawable.ic_security_key
                    ),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray3
                )
                Text(
                    text = interactionState.label,
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1
                )
            }
        }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun signingPurposeDescription(signingPurpose: SigningPurpose) =
    when (signingPurpose) {
        SigningPurpose.SignAuth -> com.babylon.wallet.android.R.string.empty
        SigningPurpose.SignTransaction -> com.babylon.wallet.android.R.string.signing_withDeviceFactorSource_signTransaction
    }

@Composable
private fun SignatureSuccessfulContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.defaultBackground)
            .padding(RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Icon(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(GradientBrand2, blendMode = BlendMode.SrcAtop)
                    }
                },
            painter = painterResource(
                id = R.drawable.ic_security_key
            ),
            contentDescription = null
        )
        Text(
            text = stringResource(com.babylon.wallet.android.R.string.signing_signatureSuccessful_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            text = stringResource(id = com.babylon.wallet.android.R.string.signing_signatureSuccessful_body),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SignatureRequestContentPreview() {
    RadixWalletTheme {
        SignatureRequestContent(
            interactionState = InteractionState.Ledger.Pending(
                LedgerHardwareWalletFactorSource.newSource(
                    model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
                    name = "nanoS",
                    deviceID = HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc501e0010196f5")
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SignatureSuccessfulContentPreview() {
    RadixWalletTheme {
        SignatureSuccessfulContent()
    }
}
