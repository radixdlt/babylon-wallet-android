package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.hasCustomizableGuarantees
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.assets.dashedCircleBorder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun DepositAccountContent(
    modifier: Modifier = Modifier,
    to: ImmutableList<AccountWithTransferableResources>,
    promptForGuarantees: () -> Unit,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit
) {
    if (to.isNotEmpty()) {
        Column(modifier = modifier) {
            Row(verticalAlignment = Alignment.Bottom) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .dashedCircleBorder(RadixTheme.colors.gray3),
                        painter = painterResource(id = DSR.ic_arrow_received_downward),
                        contentDescription = null,
                        tint = RadixTheme.colors.gray2
                    )
                    Text(
                        text = stringResource(id = R.string.transactionReview_depositsHeading).uppercase(),
                        style = RadixTheme.typography.body1Link,
                        color = RadixTheme.colors.gray2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(top = RadixTheme.dimensions.paddingSmall)
                    .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                    .background(
                        color = Color.White,
                        shape = RadixTheme.shapes.roundedRectDefault
                    )
                    .padding(RadixTheme.dimensions.paddingMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                to.onEachIndexed { index, accountEntry ->
                    val lastItem = index == to.size - 1
                    TransactionAccountCard(
                        account = accountEntry,
                        onFungibleResourceClick = { fungibleResource, isNewlyCreated ->
                            onFungibleResourceClick(fungibleResource, isNewlyCreated)
                        },
                        onNonFungibleResourceClick = { nonFungibleResource, nonFungibleResourceItem, isNewlyCreated ->
                            onNonFungibleResourceClick(nonFungibleResource, nonFungibleResourceItem, isNewlyCreated)
                        }
                    )

                    if (!lastItem) {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                    }
                }

                if (to.hasCustomizableGuarantees()) {
                    RadixTextButton(
                        modifier = Modifier
                            .padding(top = RadixTheme.dimensions.paddingXSmall),
                        text = stringResource(id = R.string.transactionReview_customizeGuaranteesButtonTitle),
                        onClick = promptForGuarantees
                    )
                }
            }
        }
    }
}

@Composable
fun StrokeLine(
    modifier: Modifier = Modifier,
    height: Dp = 24.dp
) {
    val strokeColor = RadixTheme.colors.gray3
    val strokeWidth = with(LocalDensity.current) { 2.dp.toPx() }
    val strokeInterval = with(LocalDensity.current) { 6.dp.toPx() }
    val lineHeight = with(LocalDensity.current) { height.toPx() }
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeInterval, strokeInterval), 0f)
    Canvas(
        modifier
            .width(1.dp)
            .height(height)) {
        val width = size.width
        drawLine(
            color = strokeColor,
            start = Offset(width, 0f),
            end = Offset(width, lineHeight),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DepositAccountPreview() {
    RadixWalletTheme {
        DepositAccountContent(
            to = listOf(SampleDataProvider().accountWithTransferableResourcesOwned).toPersistentList(),
            promptForGuarantees = {},
            onFungibleResourceClick = { _, _ -> },
            onNonFungibleResourceClick = { _, _, _ -> }
        )
    }
}
