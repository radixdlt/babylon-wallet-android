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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sample

@Composable
fun BoundedAmountSection(
    modifier: Modifier = Modifier,
    boundedAmount: BoundedAmount?,
    amountStyle: TextStyle = RadixTheme.typography.body1Header,
    isCompact: Boolean = false
) {
    BoundedAmountSection(
        modifier = modifier,
        boundedAmount = boundedAmount,
        horizontalAlignment = Alignment.End,
        qualifier = { text, style, color ->
            QualifierText(
                text = text,
                fontSize = 12.sp,
                style = style,
                color = color
            )
        },
        amount = { amount ->
            AmountText(
                amount = amount,
                amountStyle = amountStyle
            )
        },
        isCompact = isCompact
    )
}

@Composable
fun LargeBoundedAmountSection(
    modifier: Modifier = Modifier,
    boundedAmount: BoundedAmount?,
    symbol: String? = null
) {
    BoundedAmountSection(
        modifier = modifier,
        boundedAmount = boundedAmount,
        horizontalAlignment = Alignment.CenterHorizontally,
        qualifier = { text, style, color ->
            QualifierText(
                text = text,
                fontSize = 16.sp,
                style = style,
                color = color
            )
        },
        amount = { amount ->
            AmountText(
                amount = amount,
                amountStyle = RadixTheme.typography.title,
                symbol = symbol,
                symbolStyle = RadixTheme.typography.secondaryHeader
            )
        },
        isCompact = false
    )
}

@Composable
fun BoundedAmountSection(
    modifier: Modifier,
    boundedAmount: BoundedAmount?,
    horizontalAlignment: Alignment.Horizontal,
    qualifier: @Composable (String, TextStyle, Color) -> Unit,
    amount: @Composable (Decimal192) -> Unit,
    isCompact: Boolean
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        when (boundedAmount) {
            is BoundedAmount.Exact -> {
                amount(boundedAmount.amount)
            }
            is BoundedAmount.Max -> {
                qualifier(
                    stringResource(id = R.string.interactionReview_noMoreThan),
                    RadixTheme.typography.body1Regular,
                    RadixTheme.colors.gray1
                )

                amount(boundedAmount.amount)
            }
            is BoundedAmount.Min -> {
                qualifier(
                    stringResource(id = R.string.interactionReview_atLeast),
                    RadixTheme.typography.body1Regular,
                    RadixTheme.colors.gray1
                )

                amount(boundedAmount.amount)
            }
            is BoundedAmount.Range -> {
                qualifier(
                    stringResource(id = R.string.interactionReview_atLeast),
                    RadixTheme.typography.body1Regular,
                    RadixTheme.colors.gray1
                )

                amount(boundedAmount.minAmount)

                qualifier(
                    stringResource(id = R.string.interactionReview_noMoreThan),
                    RadixTheme.typography.body1Regular,
                    RadixTheme.colors.gray1
                )

                amount(boundedAmount.maxAmount)
            }
            is BoundedAmount.Predicted -> {
                qualifier(
                    stringResource(id = R.string.interactionReview_estimated),
                    RadixTheme.typography.body2Link,
                    RadixTheme.colors.gray1
                )

                amount(boundedAmount.estimated)

                if (!isCompact) {
                    qualifier(
                        stringResource(id = R.string.interactionReview_guaranteed),
                        RadixTheme.typography.body2Regular,
                        RadixTheme.colors.gray2
                    )

                    amount(boundedAmount.guaranteed)
                }
            }
            else -> {}
        }
    }
}

@Composable
fun UnknownAmount(
    modifier: Modifier = Modifier,
    amount: BoundedAmount?
) {
    val unknownAmount = remember(amount) { amount as? BoundedAmount.Unknown }
    unknownAmount?.let {
        WarningText(
            modifier = modifier,
            text = AnnotatedString(stringResource(id = R.string.interactionReview_unknown_amount)),
            textStyle = RadixTheme.typography.body2HighImportance,
            contentColor = RadixTheme.colors.orange1
        )
    }
}

@Composable
private fun AmountText(
    modifier: Modifier = Modifier,
    amount: Decimal192,
    symbol: String? = null,
    amountStyle: TextStyle = RadixTheme.typography.secondaryHeader,
    symbolStyle: TextStyle = RadixTheme.typography.secondaryHeader
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
    ) {
        Text(
            text = amount.formatted(),
            style = amountStyle,
            color = RadixTheme.colors.gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        symbol?.let {
            Text(
                modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingXXSmall),
                text = it,
                style = symbolStyle,
                color = RadixTheme.colors.gray1
            )
        }
    }
}

@Composable
private fun QualifierText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    style: TextStyle = RadixTheme.typography.body1Regular,
    color: Color = RadixTheme.colors.gray1
) {
    Text(
        modifier = modifier,
        text = text,
        style = style,
        fontSize = fontSize,
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

@UsesSampleValues
class BoundedAmountSectionPreviewProvider : PreviewParameterProvider<BoundedAmount> {

    override val values: Sequence<BoundedAmount>
        get() = sequenceOf(
            BoundedAmount.Exact(Decimal192.sample()),
            BoundedAmount.Max(Decimal192.sample()),
            BoundedAmount.Min(Decimal192.sample()),
            BoundedAmount.Range(Decimal192.sample(), Decimal192.sample.other()),
            BoundedAmount.Predicted(Decimal192.sample(), 1, 0.75.toDecimal192()),
            BoundedAmount.Unknown
        )
}
