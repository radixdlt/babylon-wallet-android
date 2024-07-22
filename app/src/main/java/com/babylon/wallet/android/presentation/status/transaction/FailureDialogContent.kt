package com.babylon.wallet.android.presentation.status.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.TransactionId
import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.extensions.init

@Composable
internal fun FailureDialogContent(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    transactionId: String,
    isMobileConnect: Boolean
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
            Icon(
                modifier = Modifier.size(104.dp),
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
                ),
                contentDescription = null,
                tint = RadixTheme.colors.orange1
            )
            Text(
                text = title,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
            )

            subtitle?.let {
                Text(
                    text = it,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
            }

            val txId = remember(transactionId) {
                runCatching { IntentHash.init(transactionId) }.getOrNull()
            }
            if (txId != null) {
                TransactionId(transactionId = txId)
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

internal class FailureContentParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(true, false)
}

@Composable
@Preview
private fun SomethingWentWrongDialogPreview(@PreviewParameter(FailureContentParameterProvider::class) isMobileConnect: Boolean) {
    RadixWalletTheme {
        FailureDialogContent(
            isMobileConnect = isMobileConnect,
            title = "Title",
            subtitle = "Subtitle",
            transactionId = "rdx1239j329fj292r32e23"
        )
    }
}
