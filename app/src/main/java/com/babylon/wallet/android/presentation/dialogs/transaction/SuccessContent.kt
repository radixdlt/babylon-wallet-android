package com.babylon.wallet.android.presentation.dialogs.transaction

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
import com.babylon.wallet.android.designsystem.theme.themedColorFilter
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.TransactionId
import com.babylon.wallet.android.presentation.ui.composables.dAppDisplayName
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample

@Composable
internal fun SuccessContent(
    modifier: Modifier = Modifier,
    transactionId: TransactionIntentHash?,
    isMobileConnect: Boolean,
    isInternal: Boolean,
    dAppName: String?
) {
    SuccessContent(
        modifier = modifier,
        transactionId = transactionId,
        isMobileConnect = isMobileConnect,
        title = stringResource(id = R.string.transactionStatus_success_title),
        subtitle = if (isInternal) {
            stringResource(R.string.transactionStatus_success_text)
        } else {
            stringResource(
                id = R.string.dAppRequest_completion_subtitle,
                dAppName.dAppDisplayName()
            )
        }
    )
}

@Composable
internal fun SuccessContent(
    modifier: Modifier = Modifier,
    transactionId: TransactionIntentHash?,
    isMobileConnect: Boolean,
    title: String,
    subtitle: String
) {
    Column(
        modifier = modifier
            .background(
                color = if (isMobileConnect) {
                    RadixTheme.colors.backgroundSecondary
                } else {
                    RadixTheme.colors.background
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Image(
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
                ),
                contentDescription = null,
                colorFilter = themedColorFilter()
            )
            Text(
                text = title,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            transactionId?.let {
                TransactionId(transactionId)
            }
        }
        if (isMobileConnect) {
            HorizontalDivider(color = RadixTheme.colors.divider)
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = RadixTheme.colors.backgroundSecondary)
                    .padding(vertical = RadixTheme.dimensions.paddingLarge, horizontal = RadixTheme.dimensions.paddingXLarge),
                text = stringResource(id = R.string.mobileConnect_interactionSuccess),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
        }
    }
}

internal class SuccessContentParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(true, false)
}

@Preview
@UsesSampleValues
@Composable
private fun SuccessBottomDialogPreviewLight(
    @PreviewParameter(SuccessContentParameterProvider::class) isMobileConnect: Boolean
) {
    RadixWalletPreviewTheme {
        SuccessContent(
            transactionId = TransactionIntentHash.sample(),
            isMobileConnect = isMobileConnect,
            isInternal = false,
            dAppName = null
        )
    }
}

@Preview
@UsesSampleValues
@Composable
private fun SuccessBottomDialogPreviewDark(
    @PreviewParameter(SuccessContentParameterProvider::class) isMobileConnect: Boolean
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        SuccessContent(
            transactionId = TransactionIntentHash.sample(),
            isMobileConnect = isMobileConnect,
            isInternal = false,
            dAppName = null
        )
    }
}
