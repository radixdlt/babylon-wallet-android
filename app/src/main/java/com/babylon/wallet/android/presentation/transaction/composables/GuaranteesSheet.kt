package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.transaction.GuaranteesAccountItemUiModel
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun GuaranteesSheet(
    modifier: Modifier = Modifier,
    guaranteesAccounts: ImmutableList<GuaranteesAccountItemUiModel>,
    onClose: () -> Unit,
    onApplyClick: () -> Unit,
    onGuaranteeValueChanged: (Pair<String, GuaranteesAccountItemUiModel>) -> Unit
) {
    Column(
        modifier = modifier
            .imePadding()
    ) {
        BottomDialogDragHandle(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
                .padding(top = RadixTheme.dimensions.paddingDefault)
        )
        IconButton(onClick = onClose) {
            Icon(
                painterResource(id = R.drawable.ic_close),
                tint = RadixTheme.colors.gray1,
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = stringResource(id = com.babylon.wallet.android.R.string.customize_guarantees),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                InfoLink(
                    stringResource(com.babylon.wallet.android.R.string.how_do_guarantees_work),
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            item {
                Text(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(id = com.babylon.wallet.android.R.string.guarantees_sheet_body_text),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            items(guaranteesAccounts) { guaranteesAccount ->
                GrayBackgroundWrapper {
                    TransactionAccountWithGuaranteesCard(
                        appearanceId = guaranteesAccount.appearanceID,
                        tokenAddress = guaranteesAccount.address,
                        isTokenXrd = true,
                        tokenIconUrl = "",
                        tokenSymbol = guaranteesAccount.tokenSymbol,
                        tokenEstimatedQuantity = guaranteesAccount.tokenEstimatedQuantity,
                        tokenGuaranteedQuantity = guaranteesAccount.tokenGuaranteedQuantity,
                        modifier = Modifier.padding(top = RadixTheme.dimensions.paddingDefault),
                        accountName = guaranteesAccount.displayName,
                        guaranteePercentValue = guaranteesAccount.guaranteedPercentAmount,
                        onGuaranteeValueChanged = { percentValue ->
                            onGuaranteeValueChanged(Pair(percentValue, guaranteesAccount))
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                RadixPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = com.babylon.wallet.android.R.string.apply),
                    onClick = {
                        onApplyClick()
                    },
                    enabled = true
                )
            }
        }
    }
}

@Preview("default")
@Preview(showBackground = true)
@Composable
fun GuaranteesSheetPreview() {
    RadixWalletTheme {
        GuaranteesSheet(
            guaranteesAccounts =
            persistentListOf(
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
