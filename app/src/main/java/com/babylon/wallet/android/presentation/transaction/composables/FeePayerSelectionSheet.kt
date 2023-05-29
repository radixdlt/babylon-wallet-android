package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.transaction.GuaranteesAccountItemUiModel
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun FeePayerSelectionSheet(
    modifier: Modifier = Modifier,
    accounts: ImmutableList<AccountItemUiModel>,
    onClose: () -> Unit,
    onPayerSelected: (AccountItemUiModel) -> Unit,
    onPayerConfirmed: () -> Unit
) {
    Column(
        modifier = modifier.imePadding()
    ) {
        BottomDialogDragHandle(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
                .padding(top = RadixTheme.dimensions.paddingDefault),
            onDismissRequest = onClose
        )

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(id = com.babylon.wallet.android.R.string.no_account_to_pay_fee),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(id = com.babylon.wallet.android.R.string.pay_tx_fee, TransactionConfig.DEFAULT_LOCK_FEE),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            items(accounts) { account ->
                val gradientColor = getAccountGradientColorsFor(account.appearanceID)
                AccountSelectionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                        .background(
                            brush = Brush.horizontalGradient(gradientColor),
                            shape = RadixTheme.shapes.roundedRectSmall
                        )
                        .clip(RadixTheme.shapes.roundedRectSmall)
                        .throttleClickable {
                            onPayerSelected(account)
                        },
                    accountName = account.displayName.orEmpty(),
                    address = account.address,
                    checked = account.isSelected,
                    isSingleChoice = true,
                    radioButtonClicked = {
                        onPayerSelected(account)
                    }
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
        }
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.confirm_fee_payer),
            onClick = onPayerConfirmed,
            enabled = true
        )
    }
}

@Preview("default")
@Preview(showBackground = true)
@Composable
fun FeePayerSelectionSheetPreview() {
    RadixWalletTheme {
        GuaranteesSheet(
            guaranteesAccounts = persistentListOf(
                GuaranteesAccountItemUiModel(
                    address = "f43f43f4334",
                    appearanceID = 1,
                    displayName = "My account 1",
                    tokenSymbol = "XRD",
                    tokenIconUrl = "",
                    tokenEstimatedQuantity = "1000",
                    tokenGuaranteedQuantity = "1000",
                    guaranteedPercentAmount = "100"
                ),
                GuaranteesAccountItemUiModel(
                    address = "f43f43f4334",
                    appearanceID = 1,
                    displayName = "My account 2",
                    tokenSymbol = "XRD",
                    tokenIconUrl = "",
                    tokenEstimatedQuantity = "1000",
                    tokenGuaranteedQuantity = "1000",
                    guaranteedPercentAmount = "100"
                ),
                GuaranteesAccountItemUiModel(
                    address = "f43f43f4334",
                    appearanceID = 1,
                    displayName = "My account 3",
                    tokenSymbol = "XRD",
                    tokenIconUrl = "",
                    tokenEstimatedQuantity = "1000",
                    tokenGuaranteedQuantity = "1000",
                    guaranteedPercentAmount = "100"
                )
            ),
            onClose = {},
            onApplyClick = {},
            onGuaranteeValueChanged = {}
        )
    }
}
