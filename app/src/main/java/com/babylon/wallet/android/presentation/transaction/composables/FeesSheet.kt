package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.LabelType
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.samples.sampleMainnet

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeesSheet(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State.Sheet.CustomizeFees,
    transactionFees: TransactionFees,
    isSelectedFeePayerInvolvedInTransaction: Boolean,
    insufficientBalanceToPayTheFee: Boolean,
    onClose: () -> Unit,
    onChangeFeePayerClick: () -> Unit,
    onSelectFeePayerClick: () -> Unit,
    onFeePaddingAmountChanged: (String) -> Unit,
    onTipPercentageChanged: (String) -> Unit,
    onViewDefaultModeClick: () -> Unit,
    onViewAdvancedModeClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        stickyHeader {
            BottomDialogHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault),
                onDismissRequest = onClose
            )
        }

        item {
            val title = when (state.feesMode) {
                TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Default -> {
                    stringResource(id = R.string.customizeNetworkFees_normalMode_title)
                }

                TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                    stringResource(id = R.string.customizeNetworkFees_advancedMode_title)
                }
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                text = title,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
        }

        item {
            val body = when (state.feesMode) {
                TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Default -> {
                    stringResource(id = R.string.customizeNetworkFees_normalMode_subtitle)
                }

                TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                    stringResource(id = R.string.customizeNetworkFees_advancedMode_subtitle)
                }
            }
            Text(
                modifier = Modifier
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingXXXXLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    ),
                text = body,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                InfoButton(
                    text = stringResource(id = R.string.infoLink_title_transactionfee),
                    onClick = {
                        onInfoClick(GlossaryItem.transactionfee)
                    }
                )
            }

            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = RadixTheme.dimensions.paddingLarge,
                        start = RadixTheme.dimensions.paddingDefault,
                        end = RadixTheme.dimensions.paddingDefault
                    ),
                color = RadixTheme.colors.gray4
            )
        }

        when (val feePayer = state.feePayerMode) {
            TransactionReviewViewModel.State.Sheet.CustomizeFees.FeePayerMode.NoFeePayerRequired -> {
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.customizeNetworkFees_payFeeFrom)
                                .uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.customizeNetworkFees_changeButtonTitle
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
                            text = stringResource(id = R.string.customizeNetworkFees_noneRequired),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray2
                        )
                    }
                }
            }

            is TransactionReviewViewModel.State.Sheet.CustomizeFees.FeePayerMode.FeePayerSelected -> {
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.customizeNetworkFees_payFeeFrom)
                                .uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.customizeNetworkFees_changeButtonTitle
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

                    if (insufficientBalanceToPayTheFee) {
                        WarningText(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = RadixTheme.dimensions.paddingLarge,
                                    vertical = RadixTheme.dimensions.paddingSmall
                                ),
                            text = AnnotatedString(stringResource(id = R.string.transactionReview_feePayerValidation_insufficientBalance)),
                            contentColor = RadixTheme.colors.red1,
                            textStyle = RadixTheme.typography.body1Header
                        )
                    } else if (isSelectedFeePayerInvolvedInTransaction.not()) {
                        Row(
                            modifier = Modifier
                                .padding(
                                    horizontal = RadixTheme.dimensions.paddingLarge,
                                    vertical = RadixTheme.dimensions.paddingSmall
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                        ) {
                            WarningText(
                                modifier = Modifier.weight(1f),
                                text = AnnotatedString(stringResource(id = R.string.transactionReview_feePayerValidation_linksNewAccount)),
                                textStyle = RadixTheme.typography.body1Header
                            )
                            InfoButton(
                                text = stringResource(R.string.empty),
                                color = RadixTheme.colors.gray3,
                                onClick = {
                                    onInfoClick(GlossaryItem.payingaccount)
                                }
                            )
                        }
                    }
                }
            }

            is TransactionReviewViewModel.State.Sheet.CustomizeFees.FeePayerMode.NoFeePayerSelected -> {
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.customizeNetworkFees_payFeeFrom)
                                .uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.customizeNetworkFees_changeButtonTitle
                            ),
                            onClick = { /* Not needed since its disabled */ },
                            enabled = false
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.customizeNetworkFees_selectFeePayer_navigationTitle
                            ),
                            onClick = onSelectFeePayerClick
                        )
                    }
                }
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
                TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Default -> {
                    NetworkFeesDefaultView(
                        transactionFees = transactionFees
                    )
                }

                TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                    NetworkFeesAdvancedView(
                        transactionFees = transactionFees,
                        onFeePaddingAmountChanged = onFeePaddingAmountChanged,
                        onTipPercentageChanged = onTipPercentageChanged
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RadixTheme.dimensions.paddingXXXLarge),
                horizontalArrangement = Arrangement.Center
            ) {
                when (state.feesMode) {
                    TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Default -> {
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.customizeNetworkFees_viewAdvancedModeButtonTitle
                            ),
                            onClick = onViewAdvancedModeClick
                        )
                    }

                    TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Advanced -> {
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.customizeNetworkFees_viewNormalModeButtonTitle
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
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = RadixTheme.dimensions.paddingLarge,
                    end = RadixTheme.dimensions.paddingLarge,
                    bottom = RadixTheme.dimensions.paddingDefault
                ),
            text = stringResource(id = R.string.customizeNetworkFees_feeBreakdownTitle).uppercase(),
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.gray2
        )

        Column(
            modifier = Modifier
                .background(RadixTheme.colors.gray5)
                .padding(
                    vertical = RadixTheme.dimensions.paddingDefault,
                    horizontal = RadixTheme.dimensions.paddingXXLarge
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        ),
                    text = stringResource(id = R.string.customizeNetworkFees_networkFee).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = transactionFees?.networkFeeDisplayed?.let {
                        stringResource(id = R.string.transactionReview_xrdAmount, it)
                    } ?: stringResource(id = R.string.customizeNetworkFees_noneDue),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        ),
                    text = stringResource(id = R.string.customizeNetworkFees_royaltyFee).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))
                val royaltyFee = transactionFees?.defaultRoyaltyFeesDisplayed

                Text(
                    text = if (transactionFees?.noDefaultRoyaltiesDue == true) {
                        stringResource(id = R.string.customizeNetworkFees_noneDue)
                    } else {
                        stringResource(id = R.string.transactionReview_xrdAmount, royaltyFee.orEmpty())
                    },
                    style = RadixTheme.typography.body1Header,
                    color = if (transactionFees?.noDefaultRoyaltiesDue == true) {
                        RadixTheme.colors.gray3
                    } else {
                        RadixTheme.colors.gray1
                    },
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(
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
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        ),
                    text = stringResource(id = R.string.transactionReview_networkFee_heading).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        id = R.string.transactionReview_xrdAmount,
                        transactionFees?.defaultTransactionFee?.formatted().orEmpty()
                    ),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun NetworkFeesAdvancedView(
    modifier: Modifier = Modifier,
    transactionFees: TransactionFees?,
    onFeePaddingAmountChanged: (String) -> Unit,
    onTipPercentageChanged: (String) -> Unit
) {
    Column(
        modifier = modifier
            .padding(
                vertical = RadixTheme.dimensions.paddingMedium,
            )
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    bottom = RadixTheme.dimensions.paddingDefault,
                    start = RadixTheme.dimensions.paddingDefault,
                    end = RadixTheme.dimensions.paddingDefault,
                ),
            color = RadixTheme.colors.gray4
        )

        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = RadixTheme.dimensions.paddingMedium,
                    horizontal = RadixTheme.dimensions.paddingXXLarge
                ),
            onValueChanged = onFeePaddingAmountChanged,
            value = transactionFees?.feePaddingAmountToDisplay.orEmpty(),
            leftLabel = LabelType.Default(
                stringResource(
                    id = R.string.customizeNetworkFees_paddingFieldLabel
                )
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
                .padding(
                    vertical = RadixTheme.dimensions.paddingMedium,
                    horizontal = RadixTheme.dimensions.paddingXXLarge
                ),
            onValueChanged = onTipPercentageChanged,
            value = transactionFees?.tipPercentageToDisplay.orEmpty(),
            leftLabel = LabelType.Custom {
                Text(
                    text = stringResource(id = R.string.customizeNetworkFees_tipFieldLabel),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1
                )

                Text(
                    text = stringResource(id = R.string.customizeNetworkFees_tipFieldInfo)
                        .replace("%%", "%"),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )
            },
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

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                ),
            text = stringResource(id = R.string.customizeNetworkFees_feeBreakdownTitle).uppercase(),
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.gray2
        )

        Column(
            modifier = Modifier
                .background(RadixTheme.colors.gray5)
                .padding(
                    vertical = RadixTheme.dimensions.paddingDefault,
                    horizontal = RadixTheme.dimensions.paddingLarge
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        ),
                    text = stringResource(
                        id = R.string.customizeNetworkFees_networkExecution
                    ).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        id = R.string.transactionReview_xrdAmount,
                        transactionFees?.networkExecutionCost.orEmpty()
                    ),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        ),
                    text = stringResource(
                        id = R.string.customizeNetworkFees_networkFinalization
                    ).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        id = R.string.transactionReview_xrdAmount,
                        transactionFees?.networkFinalizationCost.orEmpty()
                    ),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        ),
                    text = stringResource(
                        id = R.string.customizeNetworkFees_effectiveTip
                    ).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))
                val effectiveTip = transactionFees?.effectiveTip?.formatted().orEmpty()
                Text(
                    text = stringResource(
                        id = R.string.transactionReview_xrdAmount,
                        effectiveTip
                    ),
                    style = RadixTheme.typography.body1Header,
                    color = if (effectiveTip == "0") RadixTheme.colors.gray3 else RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        ),
                    text = stringResource(
                        id = R.string.customizeNetworkFees_networkStorage
                    ).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        id = R.string.transactionReview_xrdAmount,
                        transactionFees?.networkStorageCost.orEmpty()
                    ),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        ),
                    text = stringResource(id = R.string.customizeNetworkFees_padding).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        id = R.string.transactionReview_xrdAmount,
                        transactionFees?.feePaddingAmountForCalculation?.formatted().orEmpty()
                    ),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        ),
                    text = stringResource(id = R.string.customizeNetworkFees_royalties).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))

                val royaltyFee = if (transactionFees?.noRoyaltiesCostDue == true) {
                    stringResource(id = R.string.customizeNetworkFees_noneDue)
                } else {
                    stringResource(
                        id = R.string.transactionReview_xrdAmount,
                        transactionFees?.royaltiesCost.orEmpty()
                    )
                }
                Text(
                    text = royaltyFee,
                    style = RadixTheme.typography.body1Header,
                    color = if (transactionFees?.noRoyaltiesCostDue == true) {
                        RadixTheme.colors.gray3
                    } else {
                        RadixTheme.colors.gray1
                    },
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        ),
                    text = stringResource(
                        id = R.string.customizeNetworkFees_paidByDApps
                    ).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(
                        id = R.string.transactionReview_xrdAmount,
                        transactionFees?.paidByDApps.orEmpty()
                    ),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(
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
                    .padding(
                        top = RadixTheme.dimensions.paddingSmall,
                        bottom = RadixTheme.dimensions.paddingDefault,
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            end = RadixTheme.dimensions.paddingMedium
                        )
                ) {
                    Text(
                        modifier = Modifier
                            .padding(end = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(
                            id = R.string.customizeNetworkFees_totalFee
                        ).uppercase(),
                        style = RadixTheme.typography.body1Link,
                        color = RadixTheme.colors.gray2
                    )
                    Text(
                        modifier = Modifier
                            .padding(end = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(
                            id = R.string.customizeNetworkFees_totalFee_info
                        ),
                        style = RadixTheme.typography.body1Link,
                        color = RadixTheme.colors.gray2
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        id = R.string.transactionReview_xrdAmount,
                        transactionFees?.transactionFeeToLock?.formatted().orEmpty()
                    ),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun FeesSheetEmptyPreview() {
    RadixWalletPreviewTheme {
        FeesSheet(
            state = TransactionReviewViewModel.State.Sheet.CustomizeFees(
                feePayerMode = TransactionReviewViewModel.State.Sheet.CustomizeFees.FeePayerMode.NoFeePayerRequired,
                feesMode = TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Default
            ),
            transactionFees = TransactionFees(),
            insufficientBalanceToPayTheFee = false,
            isSelectedFeePayerInvolvedInTransaction = false,
            onClose = {},
            onChangeFeePayerClick = {},
            onSelectFeePayerClick = {},
            onFeePaddingAmountChanged = {},
            onTipPercentageChanged = {},
            onViewDefaultModeClick = {},
            onViewAdvancedModeClick = {},
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Composable
@Preview(showBackground = true)
private fun FeesSheetNotEnoughXRDPreview() {
    RadixWalletPreviewTheme {
        FeesSheet(
            state = TransactionReviewViewModel.State.Sheet.CustomizeFees(
                feePayerMode = TransactionReviewViewModel.State.Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                    feePayerCandidate = Account.sampleMainnet.carol
                ),
                feesMode = TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Default
            ),
            transactionFees = TransactionFees(),
            insufficientBalanceToPayTheFee = true,
            isSelectedFeePayerInvolvedInTransaction = false,
            onClose = {},
            onChangeFeePayerClick = {},
            onSelectFeePayerClick = {},
            onFeePaddingAmountChanged = {},
            onTipPercentageChanged = {},
            onViewDefaultModeClick = {},
            onViewAdvancedModeClick = {},
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Composable
@Preview(showBackground = true)
private fun FeesSheetAccountNotInvolvedPreview() {
    RadixWalletPreviewTheme {
        FeesSheet(
            state = TransactionReviewViewModel.State.Sheet.CustomizeFees(
                feePayerMode = TransactionReviewViewModel.State.Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                    feePayerCandidate = Account.sampleMainnet.carol
                ),
                feesMode = TransactionReviewViewModel.State.Sheet.CustomizeFees.FeesMode.Default
            ),
            transactionFees = TransactionFees(),
            insufficientBalanceToPayTheFee = false,
            isSelectedFeePayerInvolvedInTransaction = false,
            onClose = {},
            onChangeFeePayerClick = {},
            onSelectFeePayerClick = {},
            onFeePaddingAmountChanged = {},
            onTipPercentageChanged = {},
            onViewDefaultModeClick = {},
            onViewAdvancedModeClick = {},
            onInfoClick = {}
        )
    }
}
