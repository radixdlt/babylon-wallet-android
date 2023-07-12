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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun FeePayerSelectionSheet(
    modifier: Modifier = Modifier,
    sheet: TransactionApprovalViewModel2.State.Sheet.FeePayerChooser,
    onClose: () -> Unit,
    onPayerSelected: (Network.Account) -> Unit,
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
                    text = stringResource(id = R.string.transactionReview_selectFeePayer_navigationTitle),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(
                        id = R.string.transactionReview_selectFeePayer_selectAccount,
                        TransactionConfig.DEFAULT_LOCK_FEE.toString()
                    ),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            items(sheet.candidates) { candidate ->
                val gradientColor = getAccountGradientColorsFor(candidate.appearanceID)
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
                            onPayerSelected(candidate)
                        },
                    accountName = candidate.displayName,
                    address = candidate.address,
                    checked = candidate.address == sheet.selectedCandidate?.address,
                    isSingleChoice = true,
                    radioButtonClicked = {
                        onPayerSelected(candidate)
                    }
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
        }
        RadixPrimaryButton(
            text = stringResource(id = R.string.transactionReview_selectFeePayer_confirmButton),
            onClick = onPayerConfirmed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            enabled = true
        )
    }
}
