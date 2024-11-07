package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.CountedAmount
import com.babylon.wallet.android.presentation.model.displaySubtitle
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.resources.XrdResource

@Composable
fun TransferableLsuItemContent(
    modifier: Modifier = Modifier,
    transferableLSU: Transferable.FungibleType.LSU,
    shape: Shape
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.LSU(
                modifier = Modifier.size(42.dp),
                liquidStakeUnit = transferableLSU.asset,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = transferableLSU.asset.displayTitle(),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = transferableLSU.asset.displaySubtitle(),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CountedAmountSection(countedAmount = transferableLSU.amount)
        }
        UnknownAmount(
            modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingSmall),
            amount = transferableLSU.amount
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            text = stringResource(id = R.string.interactionReview_worth).uppercase(),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2,
            maxLines = 1
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                .padding(RadixTheme.dimensions.paddingMedium)
        ) {
            Row(
                verticalAlignment = CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RadixTheme.shapes.circle),
                    tint = Color.Unspecified
                )

                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

                Text(
                    text = XrdResource.SYMBOL,
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

                Spacer(modifier = Modifier.weight(1f))

                CountedAmountSection(
                    countedAmount = transferableLSU.xrdWorth,
                    isCompact = true
                )
            }

            UnknownAmount(
                modifier = Modifier.padding(top = RadixTheme.dimensions.paddingSmall),
                amount = transferableLSU.xrdWorth
            )
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun TransferableLsuItemPreview(
    @PreviewParameter(CountedAmountSectionPreviewProvider::class) amount: CountedAmount
) {
    RadixWalletPreviewTheme {
        TransferableLsuItemContent(
            transferableLSU = Transferable.FungibleType.LSU(
                asset = LiquidStakeUnit.sampleMainnet(),
                amount = amount,
                xrdWorth = CountedAmount.Max(Decimal192.sample())
            ),
            shape = RectangleShape
        )
    }
}
