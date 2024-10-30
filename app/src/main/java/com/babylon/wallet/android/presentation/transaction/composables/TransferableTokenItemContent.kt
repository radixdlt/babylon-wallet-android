package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.FungibleAmount
import com.babylon.wallet.android.domain.model.TransferableX
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun TransferableTokenItemContent(
    modifier: Modifier = Modifier,
    transferableToken: TransferableX.FungibleType.Token,
    shape: Shape,
    isHidden: Boolean,
    hiddenResourceWarning: String
) {
    Column(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray5,
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingMedium
            )
    ) {
        Row(
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.Fungible(
                modifier = Modifier.size(44.dp),
                token = transferableToken.asset.resource,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = transferableToken.asset.displayTitle(),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FungibleAmountSection(fungibleAmount = transferableToken.amount)
        }
        TransferableHiddenItemWarning(
            isHidden = isHidden,
            text = hiddenResourceWarning
        )
    }
}

@Composable
private fun FungibleAmountSection(
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

@UsesSampleValues
@Preview(showBackground = true)
@Preview("large font", fontScale = 2f)
@Composable
private fun TransferableTokenWithExactAmountPreview() {
    RadixWalletTheme {
        TransferableTokenItemContent(
            transferableToken = TransferableX.FungibleType.Token(
                asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                amount = FungibleAmount.Exact("745".toDecimal192()),
                isNewlyCreated = false
            ),
            shape = RectangleShape,
            isHidden = false,
            hiddenResourceWarning = ""
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun TransferableTokenWithRangeAmountPreview() {
    RadixWalletTheme {
        TransferableTokenItemContent(
            transferableToken = TransferableX.FungibleType.Token(
                asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                amount = FungibleAmount.Range(minAmount = "123".toDecimal192(), maxAmount = "3564".toDecimal192()),
                isNewlyCreated = false
            ),
            shape = RectangleShape,
            isHidden = false,
            hiddenResourceWarning = ""
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun TransferableTokenWithMinAmountPreview() {
    RadixWalletTheme {
        TransferableTokenItemContent(
            transferableToken = TransferableX.FungibleType.Token(
                asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                amount = FungibleAmount.Min("10.0396".toDecimal192()),
                isNewlyCreated = false
            ),
            shape = RectangleShape,
            isHidden = false,
            hiddenResourceWarning = ""
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun TransferableTokenWithMaxAmountPreview() {
    RadixWalletTheme {
        TransferableTokenItemContent(
            transferableToken = TransferableX.FungibleType.Token(
                asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                amount = FungibleAmount.Max("10.0396".toDecimal192()),
                isNewlyCreated = false
            ),
            shape = RectangleShape,
            isHidden = false,
            hiddenResourceWarning = ""
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun TransferableTokenWithGuaranteeAmountPreview() {
    RadixWalletTheme {
        TransferableTokenItemContent(
            transferableToken = TransferableX.FungibleType.Token(
                asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                amount = FungibleAmount.Predicted(
                    amount = 69.toDecimal192(),
                    instructionIndex = 4L,
                    guaranteeOffset = "180".toDecimal192()
                ),
                isNewlyCreated = false
            ),
            shape = RectangleShape,
            isHidden = false,
            hiddenResourceWarning = ""
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun TransferableTokenWithUnknownAmountPreview() {
    RadixWalletTheme {
        TransferableTokenItemContent(
            transferableToken = TransferableX.FungibleType.Token(
                asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                amount = FungibleAmount.Unknown,
                isNewlyCreated = false
            ),
            shape = RectangleShape,
            isHidden = false,
            hiddenResourceWarning = ""
        )
    }
}
