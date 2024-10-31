package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sample

@Composable
fun FungibleAmountSection(
    modifier: Modifier = Modifier,
    fungibleAmount: FungibleAmount,
    amountTextStyle: TextStyle = RadixTheme.typography.secondaryHeader
) {
    when (fungibleAmount) {
        is FungibleAmount.Exact -> {
            AmountText(
                modifier = modifier,
                amount = fungibleAmount.amount,
                textStyle = amountTextStyle
            )
        }
        is FungibleAmount.Max -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.End
            ) {
                NoMoreThanText()
                AmountText(
                    amount = fungibleAmount.amount,
                    textStyle = amountTextStyle
                )
            }
        }
        is FungibleAmount.Min -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.End
            ) {
                AtLeastText()
                AmountText(
                    amount = fungibleAmount.amount,
                    textStyle = amountTextStyle
                )
            }
        }
        is FungibleAmount.Range -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.End
            ) {
                AtLeastText()
                AmountText(
                    amount = fungibleAmount.minAmount,
                    textStyle = amountTextStyle
                )
                NoMoreThanText()
                AmountText(
                    amount = fungibleAmount.maxAmount,
                    textStyle = amountTextStyle
                )
            }
        }
        is FungibleAmount.Predicted -> {
            Row(
                modifier = modifier,
                verticalAlignment = CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.transactionReview_estimated),
                    style = RadixTheme.typography.body2Link,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AmountText(
                    amount = fungibleAmount.amount,
                    textStyle = amountTextStyle
                )
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
                AmountText(
                    amount = fungibleAmount.guaranteeAmount,
                    textStyle = amountTextStyle
                )
            }
        }
        FungibleAmount.Unknown -> {
            WarningText(
                modifier = modifier,
                text = AnnotatedString("Amount of deposit is unknown"),
                textStyle = RadixTheme.typography.body2HighImportance,
                contentColor = RadixTheme.colors.orange1
            )
        }
    }
}

@Composable
private fun AmountText(
    modifier: Modifier = Modifier,
    amount: Decimal192,
    textStyle: TextStyle = RadixTheme.typography.secondaryHeader
) {
    Text(
        modifier = modifier,
        text = amount.formatted(),
        style = textStyle,
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
@Preview
@UsesSampleValues
private fun FungibleAmountSectionPreview(
    @PreviewParameter(FungibleAmountSectionPreviewProvider::class) fungibleAmount: FungibleAmount
) {
    RadixWalletPreviewTheme {
        FungibleAmountSection(
            fungibleAmount = fungibleAmount
        )
    }
}

@UsesSampleValues
class FungibleAmountSectionPreviewProvider : PreviewParameterProvider<FungibleAmount> {

    override val values: Sequence<FungibleAmount>
        get() = sequenceOf(
            FungibleAmount.Exact(Decimal192.sample()),
            FungibleAmount.Max(Decimal192.sample()),
            FungibleAmount.Min(Decimal192.sample()),
            FungibleAmount.Range(Decimal192.sample(), Decimal192.sample.other()),
            FungibleAmount.Predicted(Decimal192.sample(), 1, 0.75.toDecimal192()),
            FungibleAmount.Unknown
        )
}
