package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.TransactionIntentHash

@Composable
fun TransactionId(transactionId: TransactionIntentHash, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.transactionStatus_transactionID_text),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
        ActionableAddressView(
            transactionId = transactionId,
            textStyle = RadixTheme.typography.body1HighImportance,
            textColor = RadixTheme.colors.blue1,
            iconColor = RadixTheme.colors.gray2
        )
    }
}
