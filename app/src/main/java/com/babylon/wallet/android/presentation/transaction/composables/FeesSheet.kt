package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import rdx.works.core.displayableQuantity
import rdx.works.profile.data.model.pernetwork.Network

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeesSheet(
    modifier: Modifier = Modifier,
    state: TransactionApprovalViewModel.State.Sheet.CustomizeFees,
    transactionFees: TransactionFees,
    onClose: () -> Unit,
    onChangeFeePayerClick: () -> Unit,
    onSelectFeePayerClick: () -> Unit,
    onPayerSelected: (Network.Account) -> Unit,
    onNetworkAndRoyaltyFeeChanged: (String) -> Unit,
    onTipPercentageChanged: (String) -> Unit,
    onViewDefaultModeClick: () -> Unit,
    onViewAdvancedModeClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        stickyHeader {
            BottomDialogDragHandle(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
                    .padding(top = RadixTheme.dimensions.paddingDefault),
                onDismissRequest = onClose
            )
        }

        item {
            val title = when (state.feesMode) {
                TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Default -> {
                    stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_normalMode_title)
                }

                TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                    stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_advancedMode_title)
                }
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = title,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
        }

        item {
            val body = when (state.feesMode) {
                TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Default -> {
                    stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_normalMode_subtitle)
                }

                TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                    stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_advancedMode_subtitle)
                }
            }
            Text(
                modifier = Modifier
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingXXLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    ),
                text = body,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Divider(
                Modifier
                    .fillMaxWidth()
                    .padding(top = RadixTheme.dimensions.paddingLarge),
                color = RadixTheme.colors.gray4
            )
        }

        when (val feePayer = state.feePayerMode) {
            TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeePayerMode.NoFeePayerRequired -> {
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_payFeeFrom)
                                .uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.transactionReview_customizeNetworkFeeSheet_changeButtonTitle
                            ),
                            onClick = onChangeFeePayerClick
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                            .background(
                                color = RadixTheme.colors.gray5,
                                shape = RadixTheme.shapes.roundedRectMedium
                            )
                            .padding(
                                vertical = RadixTheme.dimensions.paddingMedium,
                                horizontal = RadixTheme.dimensions.paddingDefault
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_noneRequired),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray2
                        )
                    }
                }
            }
            is TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeePayerMode.FeePayerSelected -> {
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_payFeeFrom)
                                .uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.transactionReview_customizeNetworkFeeSheet_changeButtonTitle
                            ),
                            onClick = onChangeFeePayerClick
                        )
                    }
                    TransactionAccountCardHeader(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        account = AccountWithTransferableResources.Owned(
                            account = feePayer.feePayerCandidate,
                            resources = emptyList()
                        ),
                        shape = RadixTheme.shapes.roundedRectMedium
                    )
                }
            }
            is TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeePayerMode.NoFeePayerSelected -> {
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_payFeeFrom)
                                .uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.transactionReview_customizeNetworkFeeSheet_changeButtonTitle
                            ),
                            onClick = { /* Not needed since its disabled */ },
                            enabled = false
                        )
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = "No account selected",
                        style = RadixTheme.typography.body1Link,
                        color = RadixTheme.colors.gray2
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.transactionReview_customizeNetworkFeeSheet_selectFeePayerButtonTitle
                            ),
                            onClick = onSelectFeePayerClick
                        )
                    }
                }
            }
            is TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeePayerMode.SelectFeePayer -> {
                feePayerSelectionContent(
                    candidates = feePayer.candidates,
                    onPayerSelected = onPayerSelected
                )
            }
        }

        item {
            Spacer(
                modifier = Modifier
                    .height(RadixTheme.dimensions.paddingLarge)
            )
        }

        item {
            when (state.feesMode) {
                TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Default -> {
                    NetworkFeesDefaultView(
                        transactionFees = transactionFees
                    )
                }

                TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                    NetworkFeesAdvancedView(
                        transactionFees = transactionFees,
                        onNetworkAndRoyaltyFeeChanged = onNetworkAndRoyaltyFeeChanged,
                        onTipPercentageChanged = onTipPercentageChanged
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RadixTheme.dimensions.paddingDefault),
                horizontalArrangement = Arrangement.Center
            ) {
                when (state.feesMode) {
                    TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Default -> {
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.transactionReview_customizeNetworkFeeSheet_viewAdvancedModeButtonTitle
                            ),
                            onClick = onViewAdvancedModeClick
                        )
                    }

                    TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.transactionReview_customizeNetworkFeeSheet_viewNormalModeButtonTitle
                            ),
                            onClick = onViewDefaultModeClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkFeesDefaultView(
    modifier: Modifier = Modifier,
    transactionFees: TransactionFees?
) {
    Column(
        modifier = modifier
            .background(RadixTheme.colors.gray5)
            .padding(
                vertical = RadixTheme.dimensions.paddingDefault,
                horizontal = RadixTheme.dimensions.paddingXLarge
            )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                text = stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_networkFee).uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = transactionFees?.networkFeeDisplayed?.let {
                    stringResource(id = R.string.transactionReview_xrdAmount, it)
                } ?: run {
                    stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_noneDue)
                },
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        }

        Row(
            modifier = Modifier
                .padding(vertical = RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_royaltyFee).uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.weight(1f))
            val royaltyFee = transactionFees?.royaltyFeesDisplayed
            Text(
                text = if (royaltyFee == "0") {
                    stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_noneDue)
                } else {
                    stringResource(id = R.string.transactionReview_xrdAmount, royaltyFee.orEmpty())
                },
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = RadixTheme.dimensions.paddingDefault,
                    bottom = RadixTheme.dimensions.paddingSmall
                ),
            color = RadixTheme.colors.gray4
        )

        Row(
            modifier = Modifier
                .padding(vertical = RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(id = R.string.transactionReview_networkFee_heading).uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(
                    id = R.string.transactionReview_xrdAmount,
                    transactionFees?.defaultTransactionFee?.displayableQuantity().orEmpty()
                ),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        }
    }
}

@Composable
fun NetworkFeesAdvancedView(
    modifier: Modifier = Modifier,
    transactionFees: TransactionFees?,
    onNetworkAndRoyaltyFeeChanged: (String) -> Unit,
    onTipPercentageChanged: (String) -> Unit
) {
    Column(
        modifier = modifier
            .padding(
                vertical = RadixTheme.dimensions.paddingDefault,
                horizontal = RadixTheme.dimensions.paddingXLarge
            )
    ) {
        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingMedium),
            onValueChanged = onNetworkAndRoyaltyFeeChanged,
            value = transactionFees?.networkAndRoyaltyFeesToDisplay.orEmpty(),
            leftLabel = stringResource(
                id = R.string.transactionReview_customizeNetworkFeeSheet_networkRoyaltyFeesFieldLabel
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RadixTheme.colors.gray1,
                unfocusedBorderColor = RadixTheme.colors.gray3,
                focusedContainerColor = RadixTheme.colors.gray5,
                unfocusedContainerColor = RadixTheme.colors.gray5
            ),
            textStyle = RadixTheme.typography.body1Regular.copy(textAlign = TextAlign.End)
        )

        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingMedium),
            onValueChanged = onTipPercentageChanged,
            value = transactionFees?.tipPercentageToDisplay.orEmpty(),
            leftLabel = stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_tipFieldLabel),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RadixTheme.colors.gray1,
                unfocusedBorderColor = RadixTheme.colors.gray3,
                focusedContainerColor = RadixTheme.colors.gray5,
                unfocusedContainerColor = RadixTheme.colors.gray5
            ),
            textStyle = RadixTheme.typography.body1Regular.copy(textAlign = TextAlign.End)
        )

        Row(
            modifier = Modifier
                .padding(vertical = RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                modifier = Modifier
                    .padding(end = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.transactionReview_networkFee_heading).uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(
                    id = R.string.transactionReview_xrdAmount,
                    transactionFees?.transactionFeeToLock?.displayableQuantity().orEmpty()
                ),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        }
    }
}
