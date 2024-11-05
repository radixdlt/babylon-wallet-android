package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.babylon.wallet.android.presentation.model.CountedAmount
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
    countedAmount: CountedAmount?,
    amountTextStyle: TextStyle = RadixTheme.typography.secondaryHeader
) {
    when (countedAmount) {
        is CountedAmount.Exact -> {
            AmountText(
                modifier = modifier,
                amount = countedAmount.amount,
                textStyle = amountTextStyle
            )
        }
        is CountedAmount.Max -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.End
            ) {
                NoMoreThanText()
                AmountText(
                    amount = countedAmount.amount,
                    textStyle = amountTextStyle
                )
            }
        }
        is CountedAmount.Min -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.End
            ) {
                AtLeastText()
                AmountText(
                    amount = countedAmount.amount,
                    textStyle = amountTextStyle
                )
            }
        }
        is CountedAmount.Range -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.End
            ) {
                AtLeastText()
                AmountText(
                    amount = countedAmount.minAmount,
                    textStyle = amountTextStyle
                )
                NoMoreThanText()
                AmountText(
                    amount = countedAmount.maxAmount,
                    textStyle = amountTextStyle
                )
            }
        }
        is CountedAmount.Predicted -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = stringResource(id = R.string.transactionReview_estimated),
                    style = RadixTheme.typography.body2Link,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AmountText(
                    amount = countedAmount.estimated,
                    textStyle = amountTextStyle
                )
            }
        }
        CountedAmount.Unknown -> {
            WarningText(
                modifier = modifier,
                text = AnnotatedString("Amount of deposit is unknown"),
                textStyle = RadixTheme.typography.body2HighImportance,
                contentColor = RadixTheme.colors.orange1
            )
        }
        else -> {
            // TODO only symbol
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
    @PreviewParameter(FungibleAmountSectionPreviewProvider::class) countedAmount: CountedAmount
) {
    RadixWalletPreviewTheme {
        FungibleAmountSection(
            countedAmount = countedAmount
        )
    }
}

@UsesSampleValues
class FungibleAmountSectionPreviewProvider : PreviewParameterProvider<CountedAmount> {

    override val values: Sequence<CountedAmount>
        get() = sequenceOf(
            CountedAmount.Exact(Decimal192.sample()),
            CountedAmount.Max(Decimal192.sample()),
            CountedAmount.Min(Decimal192.sample()),
            CountedAmount.Range(Decimal192.sample(), Decimal192.sample.other()),
            CountedAmount.Predicted(Decimal192.sample(), 1, 0.75.toDecimal192()),
            CountedAmount.Unknown
        )
}
