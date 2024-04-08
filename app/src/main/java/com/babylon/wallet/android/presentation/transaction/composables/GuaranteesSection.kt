package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import rdx.works.core.domain.formatted

@Composable
fun GuaranteesSection(transferable: Transferable, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        val guaranteedQuantity = transferable.guaranteeAssertion as? GuaranteeAssertion.ForAmount
        Row(
            modifier = Modifier,
            verticalAlignment = CenterVertically
        ) {
            if (guaranteedQuantity != null) {
                Text(
                    modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.transactionReview_estimated),
                    style = RadixTheme.typography.body2Link,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }

            (transferable.transferable as? TransferableAsset.Fungible)?.let {
                Text(
                    modifier = Modifier,
                    text = it.amount.formatted(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }
        guaranteedQuantity?.let { quantity ->
            Row {
                Text(
                    modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.transactionReview_guaranteed),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Text(
                    modifier = Modifier,
                    text = quantity.amount.formatted(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
