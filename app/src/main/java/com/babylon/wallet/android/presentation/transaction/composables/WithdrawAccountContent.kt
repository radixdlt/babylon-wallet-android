package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.transaction.TransactionAccountItemUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun WithdrawAccountContent(
    withdrawAccounts: ImmutableList<TransactionAccountItemUiModel>,
    modifier: Modifier = Modifier
) {
    if (withdrawAccounts.isNotEmpty()) {
        Text(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.transactionReview_withdrawalsHeading).uppercase(),
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.gray2,
            overflow = TextOverflow.Ellipsis,
        )
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
            val withdrawAccountMap = withdrawAccounts.groupBy {
                it.address
            }
            withdrawAccountMap.onEachIndexed { index, accountEntry ->
                val lastItem = index == withdrawAccountMap.size - 1
                TransactionAccountCard(
                    appearanceId = accountEntry.value.first().appearanceID,
                    tokens = accountEntry.value.toPersistentList(),
                    address = accountEntry.value.first().address,
                    accountName = accountEntry.value.first().displayName
                )

                if (!lastItem) {
                    Spacer(
                        modifier = Modifier
                            .height(RadixTheme.dimensions.paddingMedium)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WithdrawAccountContentPreview() {
    RadixWalletTheme {
        WithdrawAccountContent(
            persistentListOf(
                TransactionAccountItemUiModel(
                    address = "account_tdx_19jd32jd3928jd3892jd329",
                    displayName = "My Savings Account",
                    tokenSymbol = "XRD",
                    tokenQuantity = "689.203",
                    appearanceID = 1,
                    iconUrl = "",
                    shouldPromptForGuarantees = true,
                    guaranteedQuantity = "689.203",
                    guaranteedPercentAmount = "100"
                )
            )
        )
    }
}
