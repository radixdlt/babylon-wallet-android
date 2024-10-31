package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun TransferableTokenItemContent(
    modifier: Modifier = Modifier,
    transferableToken: Transferable.FungibleType.Token,
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

@UsesSampleValues
@Preview(showBackground = true)
@Preview("large font", fontScale = 2f)
@Composable
private fun TransferableTokenWithExactAmountPreview() {
    RadixWalletTheme {
        TransferableTokenItemContent(
            transferableToken = Transferable.FungibleType.Token(
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
            transferableToken = Transferable.FungibleType.Token(
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
            transferableToken = Transferable.FungibleType.Token(
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
            transferableToken = Transferable.FungibleType.Token(
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
            transferableToken = Transferable.FungibleType.Token(
                asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                amount = FungibleAmount.Predicted(
                    estimated = 69.toDecimal192(),
                    instructionIndex = 4L,
                    percent = "180".toDecimal192()
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
            transferableToken = Transferable.FungibleType.Token(
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
