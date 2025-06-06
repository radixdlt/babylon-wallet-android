package com.babylon.wallet.android.presentation.dialogs.transaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.themedColorFilter
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.TransactionId
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample

@Composable
internal fun CompletingContent(
    modifier: Modifier = Modifier,
    transactionId: TransactionIntentHash?
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.background)
            .padding(RadixTheme.dimensions.paddingLarge)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Image(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
            ),
            alpha = 0.2F,
            contentDescription = null,
            colorFilter = themedColorFilter()
        )
        Text(
            text = stringResource(R.string.transactionStatus_completing_text),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text
        )
        if (transactionId != null) {
            TransactionId(transactionId = transactionId)
        }
        Spacer(Modifier.height(36.dp))
    }
}

@Preview()
@UsesSampleValues
@Composable
private fun CompletingBottomDialogPreviewLight() {
    RadixWalletPreviewTheme {
        CompletingContent(transactionId = TransactionIntentHash.sample())
    }
}

@Preview(showBackground = true)
@UsesSampleValues
@Composable
private fun CompletingBottomDialogPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        CompletingContent(transactionId = TransactionIntentHash.sample())
    }
}
