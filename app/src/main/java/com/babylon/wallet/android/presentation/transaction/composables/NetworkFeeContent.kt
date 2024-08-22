package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.radixdlt.sargon.extensions.formatted

@Composable
fun NetworkFeeContent(
    fees: TransactionFees,
    noFeePayerSelected: Boolean,
    insufficientBalanceToPayTheFee: Boolean,
    isSelectedFeePayerInvolvedInTransaction: Boolean,
    isNetworkFeeLoading: Boolean,
    modifier: Modifier = Modifier,
    onCustomizeClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(top = RadixTheme.dimensions.paddingDefault)
    ) {
        Row {
            Text(
                text = stringResource(id = R.string.transactionReview_networkFee_heading).uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2
            )
            // TODO later
//            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
//            Icon(
//                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline),
//                contentDescription = null,
//                tint = RadixTheme.colors.gray3
//            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .placeholder(
                        visible = isNetworkFeeLoading,
                        color = RadixTheme.colors.defaultText.copy(alpha = 0.2f),
                        shape = RadixTheme.shapes.roundedRectSmall,
                        highlight = PlaceholderHighlight.shimmer(
                            highlightColor = White
                        ),
                        placeholderFadeTransitionSpec = { tween() },
                        contentFadeTransitionSpec = { tween() }
                    ),
                text = stringResource(
                    id = R.string.transactionReview_xrdAmount,
                    fees.transactionFeeToLock.formatted()
                ),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray1
            )
        }

        if (fees.isNetworkCongested) {
            Text(
                modifier = Modifier.padding(top = RadixTheme.dimensions.paddingSmall),
                text = stringResource(id = R.string.transactionReview_networkFee_congestedText),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.orange1
            )
        }

        if (noFeePayerSelected) {
            if (!isNetworkFeeLoading) {
                InfoLink(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.transactionReview_feePayerValidation_feePayerRequired),
                    contentColor = RadixTheme.colors.orange1,
                    iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
                )
            }
        } else if (insufficientBalanceToPayTheFee) {
            InfoLink(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RadixTheme.dimensions.paddingSmall),
                text = stringResource(id = R.string.customizeNetworkFees_warning_insufficientBalance),
                contentColor = RadixTheme.colors.red1,
                iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
            )
        } else if (isSelectedFeePayerInvolvedInTransaction.not()) {
            InfoLink(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RadixTheme.dimensions.paddingSmall),
                text = stringResource(id = R.string.transactionReview_feePayerValidation_linksNewAccount),
                contentColor = RadixTheme.colors.orange1,
                iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
            )
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
            noFeePayerSelected = false,
            insufficientBalanceToPayTheFee = false,
            isSelectedFeePayerInvolvedInTransaction = true,
            isNetworkFeeLoading = true,
            onCustomizeClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentWithoutInvolvedAccountPreview() {
    RadixWalletPreviewTheme {
        NetworkFeeContent(
            fees = TransactionFees(),
            noFeePayerSelected = false,
            insufficientBalanceToPayTheFee = false,
            isSelectedFeePayerInvolvedInTransaction = false,
            isNetworkFeeLoading = false,
            onCustomizeClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentNoFeePayerPreview() {
    RadixWalletPreviewTheme {
        NetworkFeeContent(
            fees = TransactionFees(),
            noFeePayerSelected = true,
            insufficientBalanceToPayTheFee = false,
            isSelectedFeePayerInvolvedInTransaction = false,
            isNetworkFeeLoading = false,
            onCustomizeClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentInsufficientBalancePreview() {
    RadixWalletPreviewTheme {
        NetworkFeeContent(
            fees = TransactionFees(),
            noFeePayerSelected = false,
            insufficientBalanceToPayTheFee = true,
            isSelectedFeePayerInvolvedInTransaction = true,
            isNetworkFeeLoading = false,
            onCustomizeClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentInsufficientBalanceWithoutInvolvedAccountPreview() {
    RadixWalletPreviewTheme {
        NetworkFeeContent(
            fees = TransactionFees(),
            noFeePayerSelected = false,
            insufficientBalanceToPayTheFee = true,
            isSelectedFeePayerInvolvedInTransaction = false,
            isNetworkFeeLoading = false,
            onCustomizeClick = {}
        )
    }
}
