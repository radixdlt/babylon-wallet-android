package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.assets.FiatBalanceView
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.utils.Constants
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.radixdlt.sargon.extensions.formatted

@Composable
fun NetworkFeeContent(
    fees: TransactionFees,
    properties: TransactionReviewViewModel.State.Fees.Properties,
    isNetworkFeeLoading: Boolean,
    modifier: Modifier = Modifier,
    onCustomizeClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Column(
        modifier = modifier.padding(top = RadixTheme.dimensions.paddingDefault)
    ) {
        Row {
            Row(modifier = Modifier.height(intrinsicSize = IntrinsicSize.Min)) {
                Text(
                    modifier = Modifier.fillMaxHeight(),
                    text = stringResource(id = R.string.transactionReview_networkFee_heading).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXXSmall))
                InfoButton(
                    modifier = Modifier
                        .fillMaxHeight()
                        .height(1.dp),
                    text = stringResource(id = R.string.empty),
                    color = RadixTheme.colors.gray3, // TODO Theme
                    onClick = {
                        onInfoClick(GlossaryItem.transactionfee)
                    }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    modifier = Modifier
                        .radixPlaceholder(
                            visible = isNetworkFeeLoading,
                            shape = RadixTheme.shapes.roundedRectSmall
                        ),
                    text = stringResource(
                        id = R.string.transactionReview_xrdAmount,
                        fees.transactionFeeToLock.formatted()
                    ),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.text
                )
                fees.transactionFeeTotalUsd?.let { fiatPrice ->
                    FiatBalanceView(
                        fiatPrice = fiatPrice,
                        decimalPrecision = Constants.FEES_FIAT_VALUE_PRECISION,
                        isFee = true
                    )
                }
            }
        }

        if (fees.isNetworkCongested) {
            Text(
                modifier = Modifier.padding(top = RadixTheme.dimensions.paddingSmall),
                text = stringResource(id = R.string.transactionReview_networkFee_congestedText),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.warning
            )
        }

        if (!isNetworkFeeLoading) {
            if (properties.noFeePayerSelected) {
                WarningText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = RadixTheme.dimensions.paddingSmall),
                    text = AnnotatedString(stringResource(id = R.string.transactionReview_feePayerValidation_feePayerRequired)),
                )
            } else if (properties.isBalanceInsufficientToPayTheFee) {
                WarningText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = RadixTheme.dimensions.paddingSmall),
                    text = AnnotatedString(stringResource(id = R.string.customizeNetworkFees_warning_insufficientBalance)),
                    contentColor = RadixTheme.colors.error,
                    textStyle = RadixTheme.typography.body1Header
                )
            } else if (properties.isSelectedFeePayerInvolvedInTransaction.not()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = RadixTheme.dimensions.paddingSmall),
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
                        color = RadixTheme.colors.gray3, // TODO Theme
                        onClick = {
                            onInfoClick(GlossaryItem.payingaccount)
                        }
                    )
                }
            }
        }

        RadixTextButton(
            text = stringResource(id = R.string.transactionReview_networkFee_customizeButtonTitle),
            enabled = !isNetworkFeeLoading,
            isWithoutPadding = true,
            onClick = onCustomizeClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentLoadingPreview() {
    RadixWalletPreviewTheme {
        NetworkFeeContent(
            fees = TransactionFees(),
            properties = TransactionReviewViewModel.State.Fees.Properties(
                noFeePayerSelected = false,
                isBalanceInsufficientToPayTheFee = false,
                isSelectedFeePayerInvolvedInTransaction = true
            ),
            isNetworkFeeLoading = true,
            onCustomizeClick = {},
            onInfoClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentWithoutInvolvedAccountPreview() {
    RadixWalletPreviewTheme {
        NetworkFeeContent(
            fees = TransactionFees(),
            properties = TransactionReviewViewModel.State.Fees.Properties(
                noFeePayerSelected = false,
                isBalanceInsufficientToPayTheFee = false,
                isSelectedFeePayerInvolvedInTransaction = false
            ),
            isNetworkFeeLoading = false,
            onCustomizeClick = {},
            onInfoClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentNoFeePayerPreview() {
    RadixWalletPreviewTheme {
        NetworkFeeContent(
            fees = TransactionFees(),
            properties = TransactionReviewViewModel.State.Fees.Properties(
                noFeePayerSelected = true,
                isBalanceInsufficientToPayTheFee = false,
                isSelectedFeePayerInvolvedInTransaction = false
            ),
            isNetworkFeeLoading = false,
            onCustomizeClick = {},
            onInfoClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentInsufficientBalancePreview() {
    RadixWalletPreviewTheme {
        NetworkFeeContent(
            fees = TransactionFees(),
            properties = TransactionReviewViewModel.State.Fees.Properties(
                noFeePayerSelected = false,
                isBalanceInsufficientToPayTheFee = true,
                isSelectedFeePayerInvolvedInTransaction = true
            ),
            isNetworkFeeLoading = false,
            onCustomizeClick = {},
            onInfoClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentInsufficientBalanceWithoutInvolvedAccountPreview() {
    RadixWalletPreviewTheme {
        NetworkFeeContent(
            fees = TransactionFees(),
            properties = TransactionReviewViewModel.State.Fees.Properties(
                noFeePayerSelected = false,
                isBalanceInsufficientToPayTheFee = true,
                isSelectedFeePayerInvolvedInTransaction = false
            ),
            isNetworkFeeLoading = false,
            onCustomizeClick = {},
            onInfoClick = {}
        )
    }
}
