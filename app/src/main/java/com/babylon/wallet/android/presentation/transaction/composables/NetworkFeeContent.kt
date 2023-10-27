package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import rdx.works.core.displayableQuantity

@Composable
fun NetworkFeeContent(
    fees: TransactionFees,
    noFeePayerSelected: Boolean,
    insufficientBalanceToPayTheFee: Boolean,
    isNetworkFeeLoading: Boolean,
    modifier: Modifier = Modifier,
    onCustomizeClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Row(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
        ) {
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
                    id = R.string.transactionReview_xrdAmount, fees.transactionFeeToLock.displayableQuantity()
                ),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray1
            )
        }

        if (fees.isNetworkCongested) {
            Text(
                modifier = Modifier
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
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
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingSmall
                        ),
                    text = stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_selectFeePayer_warning),
                    contentColor = RadixTheme.colors.orange1,
                    iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
                )
            }
        } else if (insufficientBalanceToPayTheFee) {
            InfoLink(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                text = stringResource(id = R.string.transactionReview_customizeNetworkFeeSheet_insufficientBalance_warning),
                contentColor = RadixTheme.colors.red1,
                iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
            )
        }

        RadixTextButton(
            text = stringResource(id = R.string.transactionReview_networkFee_customizeButtonTitle),
            onClick = onCustomizeClick,
            enabled = !isNetworkFeeLoading
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkFeeContentPreview() {
    NetworkFeeContent(
        fees = TransactionFees(),
        noFeePayerSelected = false,
        insufficientBalanceToPayTheFee = false,
        isNetworkFeeLoading = false,
        onCustomizeClick = {}
    )
}
