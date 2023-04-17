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
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.transaction.PreviewAccountItemsUiModel
import com.babylon.wallet.android.presentation.transaction.TransactionAccountItemUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun WithdrawAccountContent(
    previewAccounts: ImmutableList<PreviewAccountItemsUiModel>,
    modifier: Modifier = Modifier
) {
    if (previewAccounts.isNotEmpty()) {
        Text(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.withdrawing).uppercase(),
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
            previewAccounts.forEachIndexed { index, previewAccount ->
                val lastItem = index == previewAccounts.lastIndex

                val tokens = previewAccount.accounts.map { account ->
                    TokenUiModel(
                        id = account.address,
                        iconUrl = "",
                        symbol = account.tokenSymbol,
                        tokenQuantity = account.tokenQuantityDecimal,
                        address = account.address,
                        isTokenAmountVisible = account.isTokenAmountVisible,
                        guaranteedQuantity = account.guaranteedQuantityDecimal
                    )
                }.toPersistentList()

                TransactionAccountCard(
                    appearanceId = previewAccount.appearanceID,
                    tokens = tokens,
                    address = previewAccount.address,
                    accountName = previewAccount.accountName
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
    WithdrawAccountContent(
        persistentListOf(
            PreviewAccountItemsUiModel(
                address = "account_tdx_19jd32jd3928jd3892jd329",
                accountName = "My main account",
                appearanceID = 1,
                accounts = listOf(
                    TransactionAccountItemUiModel(
                        "account_tdx_19jd32jd3928jd3892jd329",
                        "My main account",
                        "XRD",
                        "200",
                        1,
                        "",
                        isTokenAmountVisible = true,
                        shouldPromptForGuarantees = false,
                        guaranteedQuantity = "200"
                    )
                )
            )
        )
    )
}
