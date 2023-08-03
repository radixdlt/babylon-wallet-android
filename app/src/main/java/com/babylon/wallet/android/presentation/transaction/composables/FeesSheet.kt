package com.babylon.wallet.android.presentation.transaction.composables

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel
import com.babylon.wallet.android.presentation.transaction.TransactionFees
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import rdx.works.core.displayableQuantity
import rdx.works.profile.data.model.pernetwork.Network

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
        item {
            BottomDialogDragHandle(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
                    .padding(top = RadixTheme.dimensions.paddingDefault),
                onDismissRequest = onClose
            )
            val title = when (state.feesMode) {
                TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Default -> {
                    "Customize Fees"
                }

                TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                    "Advanced Customize Fees"
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
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }

        item {
            val body = when (state.feesMode) {
                TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Default -> {
                    "Choose what account to pay the transaction fee from, or add a “tip” to speed up your transaction if necessary."
                }

                TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                    "Fully customize fee payment for this transaction. Not recommended unless you are a developer or advanced user."
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
                    .padding(RadixTheme.dimensions.paddingDefault),
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
                            text = "Pay fee from".uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadixTextButton(
                            text = "Change",
                            onClick = onChangeFeePayerClick
                        )
                    }

                    Row(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "None required")
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
                            text = "Pay fee from".uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadixTextButton(
                            text = "Change",
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
                        shape = RadixTheme.shapes.roundedRectDefault
                    )
                }
            }
            is TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeePayerMode.NoFeePayerSelected -> {
                item {
                    RadixTextButton(
                        text = "Select Fee Payer",
                        onClick = onSelectFeePayerClick
                    )
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
            Divider(
                Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                color = RadixTheme.colors.gray4
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
                            text = "View Advanced Mode",
                            onClick = onViewAdvancedModeClick
                        )
                    }

                    TransactionApprovalViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                        RadixTextButton(
                            text = "View Normal Mode",
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
    transactionFees: TransactionFees?
) {
    Column(
        modifier = Modifier
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
                text = "Network Fee".uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${transactionFees?.defaultNetworkFee.orEmpty()} XRD",
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
                text = "Royalty fees".uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${transactionFees?.defaultRoyaltyFee.orEmpty()} XRD",
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
                text = "Tip".uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${transactionFees?.defaultTipToDisplay} XRD",
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            color = RadixTheme.colors.gray4
        )

        Row(
            modifier = Modifier
                .padding(vertical = RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                modifier = Modifier,
                text = "Transaction fee".uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${transactionFees?.defaultTransactionFee?.displayableQuantity()} XRD",
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        }
    }
}

@Composable
fun NetworkFeesAdvancedView(
    transactionFees: TransactionFees?,
    onNetworkAndRoyaltyFeeChanged: (String) -> Unit,
    onTipPercentageChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
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
            leftLabel = "XRD to Lock for Network and Loyalty Fees",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RadixTheme.colors.gray1,
                unfocusedBorderColor = RadixTheme.colors.gray3,
                focusedContainerColor = RadixTheme.colors.gray5,
                unfocusedContainerColor = RadixTheme.colors.gray5
            )
        )

        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingMedium),
            onValueChanged = onTipPercentageChanged,
            value = transactionFees?.tipPercentageToDisplay.orEmpty(),
            leftLabel = "Tip to Lock(% of the Network Fee)",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RadixTheme.colors.gray1,
                unfocusedBorderColor = RadixTheme.colors.gray3,
                focusedContainerColor = RadixTheme.colors.gray5,
                unfocusedContainerColor = RadixTheme.colors.gray5
            )
        )

        Row(
            modifier = Modifier
                .padding(vertical = RadixTheme.dimensions.paddingMedium)
        ) {
            Text(
                modifier = Modifier
                    .padding(end = RadixTheme.dimensions.paddingDefault),
                text = "Transaction fee".uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${transactionFees?.transactionFeeToLock?.displayableQuantity()} XRD",
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        }
    }
}
