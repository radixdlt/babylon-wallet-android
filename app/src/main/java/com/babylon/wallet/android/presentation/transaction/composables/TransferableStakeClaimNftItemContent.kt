package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.babylon.wallet.android.presentation.model.displaySubtitle
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun TransferableStakeClaimNftItemContent(
    modifier: Modifier = Modifier,
    transferableStakeClaim: Transferable.NonFungibleType.StakeClaim,
    shape: Shape,
    onClick: (Transferable.NonFungibleType.StakeClaim, Resource.NonFungibleResource.Item) -> Unit
) {
    Column(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray5,
                shape = shape
            )
            .padding(vertical = RadixTheme.dimensions.paddingMedium)
    ) {
        transferableStakeClaim.asset.resource.items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier.throttleClickable {
                    onClick(
                        transferableStakeClaim,
                        item
                    )
                }
            ) {
                TransferableStakeClaimItemHeader(
                    transferableStakeClaim = transferableStakeClaim,
                    isEstimated = remember(transferableStakeClaim.amount, item) {
                        transferableStakeClaim.amount.isPredicted(item)
                    },
                    additionalAmount = null
                )

                Column(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = RadixTheme.dimensions.paddingSmall),
                        text = stringResource(id = R.string.interactionReview_toBeClaimed).uppercase(),
                        style = RadixTheme.typography.body2HighImportance,
                        color = RadixTheme.colors.gray2,
                        maxLines = 1
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = RadixTheme.colors.gray3,
                                shape = RadixTheme.shapes.roundedRectSmall
                            )
                            .padding(RadixTheme.dimensions.paddingMedium)
                    ) {
                        Row(
                            verticalAlignment = CenterVertically,
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

                            Text(
                                modifier = Modifier.weight(1f),
                                text = item.claimAmountXrd?.formatted().orEmpty(),
                                style = RadixTheme.typography.body1HighImportance,
                                color = RadixTheme.colors.gray1,
                                textAlign = TextAlign.End,
                                maxLines = 2
                            )
                        }

                        if (item.claimAmountXrd == null) {
                            UnknownAmount(
                                modifier = Modifier
                                    .padding(top = RadixTheme.dimensions.paddingSmall),
                                amount = BoundedAmount.Unknown
                            )
                        }
                    }
                }
            }

            val addDivider = remember(index) { index != transferableStakeClaim.asset.resource.items.lastIndex }

            if (addDivider) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                HorizontalDivider(color = RadixTheme.colors.gray4)

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }
        }
    }
}

@Composable
fun TransferableStakeClaimItemHeader(
    modifier: Modifier = Modifier,
    transferableStakeClaim: Transferable.NonFungibleType.StakeClaim,
    isEstimated: Boolean,
    additionalAmount: BoundedAmount?
) {
    Column(
        modifier = modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = CenterVertically
        ) {
            Thumbnail.NonFungible(
                modifier = Modifier.size(44.dp),
                collection = transferableStakeClaim.asset.resource
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = transferableStakeClaim.asset.displayTitle(),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = transferableStakeClaim.asset.displaySubtitle(),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isEstimated) {
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                    text = stringResource(R.string.interactionReview_estimated),
                    color = RadixTheme.colors.gray1,
                    style = RadixTheme.typography.body3Regular
                )
            } else {
                additionalAmount?.let {
                    Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

                    BoundedAmountSection(boundedAmount = it)
                }
            }
        }

        UnknownAmount(
            modifier = Modifier.padding(top = RadixTheme.dimensions.paddingSmall),
            amount = additionalAmount
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun TransferableStakeClaimNftItemPreview() {
    RadixWalletTheme {
        val asset = remember {
            StakeClaim(
                nonFungibleResource = Resource.NonFungibleResource.sampleMainnet(),
                validator = Validator.sampleMainnet()
            )
        }
        val nonFungibleAmount = remember(asset) {
            NonFungibleAmount(
                certain = asset.nonFungibleResource.items
            )
        }
        TransferableStakeClaimNftItemContent(
            transferableStakeClaim = Transferable.NonFungibleType.StakeClaim(
                asset = asset,
                amount = nonFungibleAmount,
                isNewlyCreated = false
            ),
            shape = RectangleShape,
            onClick = { _, _ -> }
        )
    }
}
