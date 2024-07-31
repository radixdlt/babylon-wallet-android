package com.babylon.wallet.android.presentation.status.transaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.TransactionId
import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample

@Composable
internal fun SuccessContent(
    modifier: Modifier = Modifier,
    transactionId: IntentHash?,
    isMobileConnect: Boolean,
    isInternal: Boolean,
    dAppName: String?
) {
    Column {
        Column(
            modifier
                .fillMaxWidth()
                .background(color = RadixTheme.colors.defaultBackground)
                .padding(RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Image(
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
                ),
                contentDescription = null
            )
            Text(
                text = stringResource(id = R.string.transactionStatus_success_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            val subtitle = if (isInternal) {
                stringResource(R.string.transactionStatus_success_text)
            } else {
                stringResource(
                    id = R.string.dAppRequest_completion_subtitle,
                    dAppName.orEmpty().ifEmpty {
                        stringResource(id = R.string.dAppRequest_metadata_unknownName)
                    }
                )
            }
            Text(
                text = subtitle,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            transactionId?.let {
                TransactionId(transactionId)
            }
        }
        if (isMobileConnect) {
            HorizontalDivider(color = RadixTheme.colors.gray4)
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = RadixTheme.colors.gray5)
                    .padding(vertical = RadixTheme.dimensions.paddingLarge, horizontal = RadixTheme.dimensions.paddingXLarge),
                text = stringResource(id = R.string.mobileConnect_interactionSuccess),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
        }
    }
}

internal class SuccessContentParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(true, false)
}

@Preview(showBackground = true)
@UsesSampleValues
@Composable
private fun SuccessBottomDialogPreview(
    @PreviewParameter(SuccessContentParameterProvider::class) isMobileConnect: Boolean
) {
    RadixWalletTheme {
        SuccessContent(
            transactionId = IntentHash.sample(),
            isMobileConnect = isMobileConnect,
            isInternal = false,
            dAppName = null
        )
    }
}
