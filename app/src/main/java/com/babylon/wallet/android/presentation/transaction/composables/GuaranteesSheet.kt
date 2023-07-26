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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.AccountWithPredictedGuarantee
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel.State
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.InfoLink

@Composable
fun GuaranteesSheet(
    modifier: Modifier = Modifier,
    state: State.Sheet.CustomizeGuarantees,
    onClose: () -> Unit,
    onApplyClick: () -> Unit,
    onGuaranteeValueChanged: (AccountWithPredictedGuarantee, String) -> Unit,
    onGuaranteeValueIncreased: (AccountWithPredictedGuarantee) -> Unit,
    onGuaranteeValueDecreased: (AccountWithPredictedGuarantee) -> Unit,
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
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
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
//            item {
//                InfoLink(
//                    stringResource(com.babylon.wallet.android.R.string.transactionReview_guarantees_howDoGuaranteesWork),
//                    modifier = Modifier
//                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
//                )
//                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
//            }
            item {
                Text(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(id = com.babylon.wallet.android.R.string.transactionReview_guarantees_subtitle),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            items(state.accountsWithPredictedGuarantees) { accountWithCustomizableGuarantee ->
                GrayBackgroundWrapper {
                    TransactionAccountWithGuaranteesCard(
                        accountWithGuarantee = accountWithCustomizableGuarantee,
                        onGuaranteePercentChanged = {
                            onGuaranteeValueChanged(accountWithCustomizableGuarantee, it)
                        },
                        onGuaranteePercentIncreased = {
                            onGuaranteeValueIncreased(accountWithCustomizableGuarantee)
                        },
                        onGuaranteePercentDecreased = {
                            onGuaranteeValueDecreased(accountWithCustomizableGuarantee)
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
                    enabled = true
                )
            }
        }
    }
}
