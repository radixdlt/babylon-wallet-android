package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.TransactionFees
import com.radixdlt.ret.Decimal
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun NetworkFeeContent(
    fees: TransactionFees,
    modifier: Modifier = Modifier,
    onCustomizeClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = RadixTheme.colors.gray4
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        Row(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(id = R.string.transactionReview_networkFee_heading).uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline),
                contentDescription = null,
                tint = RadixTheme.colors.gray3
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(id = R.string.transactionReview_xrdAmount, fees.defaultTransactionFee.displayableQuantity()),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray1
            )
        }

        if (fees.isNetworkCongested) {
            Text(
                modifier = Modifier
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                text = stringResource(id = R.string.transactionReview_networkFee_congestedText),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.orange1
            )
        }

        RadixTextButton(
            text = stringResource(id = R.string.transactionReview_networkFee_customizeButtonTitle),
            onClick = onCustomizeClick
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentPreview() {
    NetworkFeeContent(
        fees = TransactionFees(
            networkFee = BigDecimal("10")
        ),
        onCustomizeClick = {}
    )
}
