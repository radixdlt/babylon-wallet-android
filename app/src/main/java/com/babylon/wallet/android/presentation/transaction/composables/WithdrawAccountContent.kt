package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.assets.dashedCircleBorder
import kotlinx.collections.immutable.ImmutableList

@Composable
fun WithdrawAccountContent(
    modifier: Modifier = Modifier,
    from: ImmutableList<AccountWithTransferableResources>,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit
) {
    if (from.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RadixTheme.dimensions.paddingDefault)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .dashedCircleBorder(RadixTheme.colors.gray3),
                painter = painterResource(id = DSR.ic_arrow_sent_upwards),
                contentDescription = null,
                tint = RadixTheme.colors.gray2
            )
            Text(
                text = stringResource(id = R.string.transactionReview_withdrawalsHeading).uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            modifier = modifier
                .padding(vertical = RadixTheme.dimensions.paddingSmall)
                .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                .background(
                    color = Color.White,
                    shape = RadixTheme.shapes.roundedRectDefault
                )
                .padding(RadixTheme.dimensions.paddingMedium)
        ) {
            from.forEachIndexed { index, account ->
                TransactionAccountCard(
                    account = account,
                    onFungibleResourceClick = { fungibleResource, isNewlyCreated ->
                        onFungibleResourceClick(fungibleResource, isNewlyCreated)
                    },
                    onNonFungibleResourceClick = { nonFungibleResource, nonFungibleResourceItem, isNewlyCreated ->
                        onNonFungibleResourceClick(nonFungibleResource, nonFungibleResourceItem, isNewlyCreated)
                    }
                )

                if (index != from.lastIndex) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                }
            }
        }
    }
}
