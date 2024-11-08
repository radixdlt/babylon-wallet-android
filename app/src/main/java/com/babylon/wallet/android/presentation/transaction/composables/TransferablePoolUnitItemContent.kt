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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.model.displaySubtitle
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.model.displayTitleAsToken
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.domain.assets.PoolUnit

@Composable
fun TransferablePoolUnitItemContent(
    modifier: Modifier = Modifier,
    transferablePoolUnit: Transferable.FungibleType.PoolUnit,
    shape: Shape,
    isHidden: Boolean,
    hiddenResourceWarning: String,
    onClick: (poolUnit: Transferable.FungibleType.PoolUnit) -> Unit
) {
    Column(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .throttleClickable {
                onClick(transferablePoolUnit)
            }
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
            Thumbnail.PoolUnit(
                modifier = Modifier.size(42.dp),
                poolUnit = transferablePoolUnit.asset
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = transferablePoolUnit.displayTitle(),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = transferablePoolUnit.asset.displaySubtitle(),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BoundedAmountSection(boundedAmount = transferablePoolUnit.amount)
        }

        UnknownAmount(
            modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingSmall),
            amount = transferablePoolUnit.amount
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

        val poolResources = transferablePoolUnit.asset.pool?.resources.orEmpty()

        Column(
            modifier = Modifier.border(
                width = 1.dp,
                color = RadixTheme.colors.gray3,
                shape = RadixTheme.shapes.roundedRectSmall
            )
        ) {
            poolResources.forEachIndexed { index, item ->
                val addDivider = index != poolResources.lastIndex
                val contributionPerResourceAmount = transferablePoolUnit.contributions[item.address]

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = RadixTheme.dimensions.paddingMedium,
                            horizontal = RadixTheme.dimensions.paddingDefault
                        )
                ) {
                    Row(
                        verticalAlignment = CenterVertically
                    ) {
                        Thumbnail.Fungible(
                            modifier = Modifier.size(24.dp),
                            token = item,
                        )

                        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

                        Text(
                            text = item.displayTitleAsToken(),
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.gray1,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

                        Spacer(modifier = Modifier.weight(1f))

                        if (contributionPerResourceAmount != null) {
                            BoundedAmountSection(
                                boundedAmount = contributionPerResourceAmount,
                                isCompact = true
                            )
                        } else {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = stringResource(id = R.string.empty),
                                style = RadixTheme.typography.body1HighImportance,
                                color = RadixTheme.colors.gray1,
                                textAlign = TextAlign.End,
                                maxLines = 2
                            )
                        }
                    }

                    UnknownAmount(
                        modifier = Modifier.padding(top = RadixTheme.dimensions.paddingSmall),
                        amount = contributionPerResourceAmount
                    )
                }
                if (addDivider) {
                    HorizontalDivider(color = RadixTheme.colors.gray3)
                }
            }
        }
        TransferableHiddenItemWarning(
            isHidden = isHidden,
            text = hiddenResourceWarning
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun TransferablePoolUnitItemPreview(
    @PreviewParameter(BoundedAmountSectionPreviewProvider::class) amount: BoundedAmount
) {
    RadixWalletPreviewTheme {
        TransferablePoolUnitItemContent(
            transferablePoolUnit = Transferable.FungibleType.PoolUnit(
                asset = PoolUnit.sampleMainnet(),
                amount = amount
            ),
            shape = RadixTheme.shapes.roundedRectDefault,
            isHidden = false,
            hiddenResourceWarning = stringResource(id = R.string.interactionReview_hiddenAsset_deposit),
            onClick = {}
        )
    }
}
