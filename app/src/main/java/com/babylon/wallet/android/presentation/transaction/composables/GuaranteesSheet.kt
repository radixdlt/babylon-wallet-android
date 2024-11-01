package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.transaction.model.GuaranteeItem
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun GuaranteesSheet(
    modifier: Modifier = Modifier,
    state: State.Sheet.CustomizeGuarantees,
    onClose: () -> Unit,
    onApplyClick: () -> Unit,
    onGuaranteeValueChanged: (GuaranteeItem, String) -> Unit,
    onGuaranteeValueIncreased: (GuaranteeItem) -> Unit,
    onGuaranteeValueDecreased: (GuaranteeItem) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        BottomDialogHeader(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
                .padding(top = RadixTheme.dimensions.paddingDefault),
            onDismissRequest = onClose
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
            text = stringResource(id = com.babylon.wallet.android.R.string.transactionReview_guarantees_title),
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
                InfoButton(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(bottom = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.infoLink_title_guarantees),
                    onClick = {
                        onInfoClick(GlossaryItem.guarantees)
                    }
                )
            }
            item {
                Text(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                    text = stringResource(
                        id = com.babylon.wallet.android.R.string.transactionReview_guarantees_subtitle
                    ),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            }
            items(state.guarantees) { guaranteeItem ->
                GrayBackgroundWrapper {
                    TransactionAccountWithGuaranteesCard(
                        modifier = Modifier
                            .padding(top = RadixTheme.dimensions.paddingLarge)
                            .padding(bottom = RadixTheme.dimensions.paddingSmall)
                            .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                        guaranteeItem = guaranteeItem,
                        onGuaranteePercentChanged = {
                            onGuaranteeValueChanged(guaranteeItem, it)
                        },
                        onGuaranteePercentIncreased = {
                            onGuaranteeValueIncreased(guaranteeItem)
                        },
                        onGuaranteePercentDecreased = {
                            onGuaranteeValueDecreased(guaranteeItem)
                        },
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                RadixPrimaryButton(
                    text = stringResource(id = com.babylon.wallet.android.R.string.transactionReview_guarantees_applyButtonText),
                    onClick = {
                        onApplyClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    enabled = state.isSubmitEnabled
                )
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun GuaranteesSheetPreview() {
    RadixWalletPreviewTheme {
        GuaranteesSheet(
            state = State.Sheet.CustomizeGuarantees(
                listOf(
                    GuaranteeItem(
                        account = InvolvedAccount.Owned(Account.sampleMainnet()),
                        transferable = Transferable.FungibleType.Token(
                            asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                            amount = FungibleAmount.Predicted(
                                estimated = 10.toDecimal192(),
                                instructionIndex = 1L,
                                offset = 1.toDecimal192()
                            ),
                            isNewlyCreated = false
                        ),
                        typedPercent = "90"
                    )
                )
            ),
            onClose = {},
            onApplyClick = {},
            onGuaranteeValueChanged = { _, _ -> },
            onGuaranteeValueDecreased = {},
            onGuaranteeValueIncreased = {},
            onInfoClick = {}
        )
    }
}
