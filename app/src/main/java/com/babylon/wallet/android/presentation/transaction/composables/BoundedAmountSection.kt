package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sample

@Composable
fun BoundedAmountSection(
    modifier: Modifier = Modifier,
    boundedAmount: BoundedAmount?,
    amountStyle: TextStyle = RadixTheme.typography.secondaryHeader,
    isCompact: Boolean = false
) {
    if (boundedAmount is BoundedAmount.Predicted) {
        PredictedAmount(
            modifier = modifier,
            amount = boundedAmount,
            isCompact = isCompact,
            horizontalAlignment = Alignment.End,
            estimatedTitleStyle = RadixTheme.typography.body2HighImportance,
            estimatedAmountStyle = RadixTheme.typography.secondaryHeader,
            guaranteedTitleStyle = RadixTheme.typography.body2Regular,
            guaranteedAmountStyle = RadixTheme.typography.body1Header.copy(
                fontSize = 14.sp
            ),
            guaranteedAmountColor = RadixTheme.colors.gray2
        )
    } else {
        BoundedAmountSection(
            modifier = modifier,
            boundedAmount = boundedAmount,
            horizontalAlignment = Alignment.End,
            qualifier = { text ->
                QualifierText(
                    text = text,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            amount = { amount ->
                AmountText(
                    amount = amount,
                    amountStyle = amountStyle
                )
            }
        )
    }
}

@Composable
fun LargeBoundedAmountSection(
    modifier: Modifier = Modifier,
    boundedAmount: BoundedAmount?,
    symbol: String? = null
) {
    if (boundedAmount is BoundedAmount.Predicted) {
        PredictedAmount(
            modifier = modifier,
            amount = boundedAmount,
            symbol = symbol,
            isCompact = false,
            horizontalAlignment = Alignment.CenterHorizontally,
            estimatedTitleStyle = RadixTheme.typography.body1HighImportance,
            estimatedAmountStyle = RadixTheme.typography.title,
            guaranteedTitleStyle = RadixTheme.typography.body2HighImportance,
            guaranteedAmountStyle = RadixTheme.typography.secondaryHeader,
            guaranteedAmountColor = RadixTheme.colors.gray2,
            symbolStyle = RadixTheme.typography.body2Link
        )
    } else {
        BoundedAmountSection(
            modifier = modifier,
            boundedAmount = boundedAmount,
            horizontalAlignment = Alignment.CenterHorizontally,
            qualifier = { text ->
                QualifierText(
                    text = text,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1
                )
            },
            amount = { amount ->
                AmountText(
                    amount = amount,
                    amountStyle = RadixTheme.typography.title,
                    symbol = symbol,
                    symbolStyle = RadixTheme.typography.secondaryHeader
                )
            }
        )
    }
}

@Composable
fun BoundedAmountSection(
    modifier: Modifier,
    boundedAmount: BoundedAmount?,
    horizontalAlignment: Alignment.Horizontal,
    qualifier: @Composable (String) -> Unit,
    amount: @Composable (Decimal192) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        when (boundedAmount) {
            is BoundedAmount.Exact -> {
                amount(boundedAmount.amount)
            }
            is BoundedAmount.Min -> {
                qualifier(stringResource(id = R.string.interactionReview_atLeast))

                amount(boundedAmount.amount)
            }
            is BoundedAmount.Range -> {
                qualifier(stringResource(id = R.string.interactionReview_atLeast))

                amount(boundedAmount.minAmount)

                qualifier(stringResource(id = R.string.interactionReview_noMoreThan))

                amount(boundedAmount.maxAmount)
            }
            else -> {}
        }
    }
}

@Composable
private fun PredictedAmount(
    modifier: Modifier = Modifier,
    amount: BoundedAmount.Predicted,
    isCompact: Boolean,
    horizontalAlignment: Alignment.Horizontal,
    estimatedTitleStyle: TextStyle,
    estimatedAmountStyle: TextStyle,
    guaranteedTitleStyle: TextStyle,
    guaranteedAmountStyle: TextStyle,
    guaranteedAmountColor: Color,
    symbol: String? = null,
    symbolStyle: TextStyle = RadixTheme.typography.body2Link,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        QualifierText(
            text = stringResource(id = R.string.interactionReview_estimated),
            style = estimatedTitleStyle,
            color = RadixTheme.colors.gray1
        )

        AmountText(
            amount = amount.estimated,
            amountStyle = estimatedAmountStyle,
            symbol = symbol,
            symbolStyle = symbolStyle
        )

        if (!isCompact) {
            QualifierText(
                text = stringResource(id = R.string.interactionReview_guaranteed),
                style = guaranteedTitleStyle,
                color = RadixTheme.colors.gray2
            )

            AmountText(
                amount = amount.guaranteed,
                amountStyle = guaranteedAmountStyle,
                amountColor = guaranteedAmountColor,
                symbol = symbol,
                symbolStyle = symbolStyle,
                symbolColor = guaranteedAmountColor
            )
        }
    }
}

@Composable
fun UnknownAmount(
    modifier: Modifier = Modifier,
    amount: BoundedAmount?
) {
    val unknownAmount = remember(amount) {
        BoundedAmount.Unknown.takeIf { amount is BoundedAmount.Unknown || amount is BoundedAmount.Max }
    }
    unknownAmount?.let {
        Text(
            modifier = modifier,
            text = stringResource(id = R.string.interactionReview_unknown_amount),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2
        )
    }
}

@Composable
private fun AmountText(
    modifier: Modifier = Modifier,
    amount: Decimal192,
    amountStyle: TextStyle = RadixTheme.typography.secondaryHeader,
    amountColor: Color = RadixTheme.colors.gray1,
    symbol: String? = null,
    symbolStyle: TextStyle = RadixTheme.typography.secondaryHeader,
    symbolColor: Color = RadixTheme.colors.gray1
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
    ) {
        Text(
            text = amount.formatted(),
            style = amountStyle,
            color = amountColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        symbol?.let {
            Text(
                modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingXXXSmall),
                text = it,
                style = symbolStyle,
                color = symbolColor
            )
        }
    }
}

@Composable
private fun QualifierText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = RadixTheme.typography.body2Regular,
    color: Color = RadixTheme.colors.gray1
) {
    Text(
        modifier = modifier,
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
@Preview
@UsesSampleValues
private fun BoundedAmountSectionPreview(
    @PreviewParameter(BoundedAmountSectionPreviewProvider::class) boundedAmount: BoundedAmount
) {
    RadixWalletPreviewTheme {
        BoundedAmountSection(
            boundedAmount = boundedAmount
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun LargeBoundedAmountSectionPreview(
    @PreviewParameter(BoundedAmountSectionPreviewProvider::class) boundedAmount: BoundedAmount
) {
    RadixWalletPreviewTheme {
        LargeBoundedAmountSection(
            boundedAmount = boundedAmount,
            symbol = "SYMBOL"
        )
    }
}

@Composable
@Preview
private fun UnknownAmountSectionPreview() {
    RadixWalletPreviewTheme {
        UnknownAmount(
            amount = BoundedAmount.Unknown
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun UnknownMaxAmountSectionPreview() {
    RadixWalletPreviewTheme {
        UnknownAmount(
            amount = BoundedAmount.Max(Decimal192.sample())
        )
    }
}

@UsesSampleValues
class BoundedAmountSectionPreviewProvider : PreviewParameterProvider<BoundedAmount> {

    override val values: Sequence<BoundedAmount>
        get() = sequenceOf(
            BoundedAmount.Exact(Decimal192.sample()),
            BoundedAmount.Min(Decimal192.sample()),
            BoundedAmount.Range(Decimal192.sample(), Decimal192.sample.other()),
            BoundedAmount.Predicted(Decimal192.sample(), 1, 0.75.toDecimal192())
        )
}
