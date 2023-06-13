package com.babylon.wallet.android.presentation.ui.composables.resultdialog.signing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.babylon.wallet.android.data.transaction.SigningState
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.GradientBrand2
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.SomethingWentWrongDialogContent
import com.babylon.wallet.android.utils.formattedSpans
import rdx.works.profile.data.model.factorsources.FactorSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigningStatusBottomDialog(
    modifier: Modifier = Modifier,
    onDismissDialogClick: () -> Unit,
    signingState: SigningState
) {
    val dismissHandler = {
        onDismissDialogClick()
    }
    BottomSheetDialogWrapper(
        onDismissRequest = dismissHandler
    ) {
        SigningStatusBottomDialogContent(
            modifier = modifier,
            signingState = signingState
        )
    }
}

@Composable
fun SigningStatusBottomDialogContent(
    modifier: Modifier = Modifier,
    signingState: SigningState?
) {
    when (signingState) {
        is SigningState.Ledger.Failure,
        is SigningState.Device.Failure -> {
            SomethingWentWrongDialogContent(
                title = stringResource(id = com.babylon.wallet.android.R.string.common_somethingWentWrong),
                subtitle = stringResource(id = com.babylon.wallet.android.R.string.common_somethingWentWrong),
                modifier = modifier
            )
        }

        is SigningState.Ledger.Success,
        is SigningState.Device.Success -> {
            SignatureSuccessfulContent(modifier = modifier)
        }

        is SigningState.Device.Pending,
        is SigningState.Ledger.Pending -> {
            SignatureRequestContent(signingState, modifier)
        }

        else -> {}
    }
}

@Composable
private fun SignatureRequestContent(
    signingState: SigningState?,
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
        Text(
            text = stringResource(com.babylon.wallet.android.R.string.signing_signatureRequest_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        val subtitle = when (signingState) {
            is SigningState.Device.Failure,
            is SigningState.Device.Success,
            is SigningState.Device.Pending -> {
                com.babylon.wallet.android.R.string.signing_withDeviceFactorSource_signTransaction
            }

            is SigningState.Ledger.Success,
            is SigningState.Ledger.Failure,
            is SigningState.Ledger.Pending -> {
                com.babylon.wallet.android.R.string.signing_signatureRequest_body
            }

            else -> null
        }
        Text(
            text = subtitle?.let { stringResource(it).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)) } ?: AnnotatedString(""),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        if (signingState?.usingLedger() == true) {
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
                    text = signingState.factorSource.label,
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1
                )
            }
        }
        Spacer(Modifier.height(36.dp))
    }
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
            signingState = SigningState.Ledger.Pending(
                FactorSource.ledger(
                    FactorSource.ID(""),
                    FactorSource.LedgerHardwareWallet.DeviceModel.NanoS,
                    "nanoS"
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
