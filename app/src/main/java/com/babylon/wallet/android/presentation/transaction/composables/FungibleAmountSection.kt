package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.model.FungibleAmount
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.formatted

@Composable
fun FungibleAmountSection(
    modifier: Modifier = Modifier,
    fungibleAmount: FungibleAmount
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        when (fungibleAmount) {
            is FungibleAmount.Exact -> {
                AmountText(amount = fungibleAmount.amount)
            }
            is FungibleAmount.Max -> {
                Column(horizontalAlignment = Alignment.End) {
                    NoMoreThanText()
                    AmountText(amount = fungibleAmount.amount)
                }
            }
            is FungibleAmount.Min -> {
                Column(horizontalAlignment = Alignment.End) {
                    AtLeastText()
                    AmountText(amount = fungibleAmount.amount)
                }
            }
            is FungibleAmount.Range -> {
                Column(horizontalAlignment = Alignment.End) {
                    AtLeastText()
                    AmountText(amount = fungibleAmount.minAmount)
                    NoMoreThanText()
                    AmountText(amount = fungibleAmount.maxAmount)
                }
            }
            is FungibleAmount.Predicted -> {
                Row(verticalAlignment = CenterVertically) {
                    Text(
                        modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                        text = stringResource(id = R.string.transactionReview_estimated),
                        style = RadixTheme.typography.body2Link,
                        color = RadixTheme.colors.gray1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AmountText(amount = fungibleAmount.amount)
                }
                Row {
                    Text(
                        modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                        text = stringResource(id = R.string.transactionReview_guaranteed),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AmountText(amount = fungibleAmount.guaranteeAmount)
                }
            }
            FungibleAmount.Unknown -> {
                WarningText(
                    text = AnnotatedString("Amount of deposit is unknown"),
                    textStyle = RadixTheme.typography.body2HighImportance,
                    contentColor = RadixTheme.colors.orange1
                )
            }
        }
    }
}

@Composable
private fun AmountText(
    modifier: Modifier = Modifier,
    amount: Decimal192
) {
    Text(
        modifier = modifier,
        text = amount.formatted(),
        style = RadixTheme.typography.secondaryHeader,
        color = RadixTheme.colors.gray1,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AtLeastText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = "At least",
        style = RadixTheme.typography.body1Regular,
        fontSize = 12.sp,
        color = RadixTheme.colors.gray1,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun NoMoreThanText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = "No more than",
        style = RadixTheme.typography.body1Regular,
        fontSize = 12.sp,
        color = RadixTheme.colors.gray1,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun HiddenTransferableWarning(
    modifier: Modifier = Modifier,
    isHidden: Boolean,
    text: String
) {
    if (isHidden) {
        Column(
            modifier = modifier
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            WarningText(
                modifier = Modifier.fillMaxWidth(),
                text = AnnotatedString(text),
                textStyle = RadixTheme.typography.body1Header
            )
        }
    }
}
