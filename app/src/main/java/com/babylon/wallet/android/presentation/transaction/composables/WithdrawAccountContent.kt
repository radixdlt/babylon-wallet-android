package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import kotlinx.collections.immutable.ImmutableList

@Composable
fun WithdrawAccountContent(
    modifier: Modifier = Modifier,
    from: ImmutableList<AccountWithTransferableResources>
) {
    if (from.isNotEmpty()) {
        Text(
            modifier = Modifier
                .padding(top = RadixTheme.dimensions.paddingDefault)
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
            text = stringResource(id = R.string.transactionReview_withdrawalsHeading).uppercase(),
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.gray2,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            modifier = modifier
                .padding(vertical = RadixTheme.dimensions.paddingSmall)
                .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                .background(
                    color = Color.White,
                    shape = RadixTheme.shapes.roundedRectDefault
                )
                .padding(RadixTheme.dimensions.paddingMedium)
        ) {
            from.forEachIndexed { index, account ->
                TransactionAccountCard(
                    account = account
                )

                if (index != from.lastIndex) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                }
            }
        }
        StrokeLine(height = 40.dp)
    } else {
        Spacer(modifier = Modifier.height(40.dp))
    }
}
