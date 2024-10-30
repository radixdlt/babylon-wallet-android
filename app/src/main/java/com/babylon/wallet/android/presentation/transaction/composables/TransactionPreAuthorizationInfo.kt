package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun TransactionPreAuthorizationInfo(
    modifier: Modifier = Modifier,
    preAuthorization: TransactionReviewViewModel.State.PreAuthorization,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Info(
            onInfoClick = {
                // TODO update to pre-auth specific glossary item
                onInfoClick(GlossaryItem.transactions)
            }
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

        Text(
            text = stringResource(id = R.string.preAuthorizationReview_expiration_atTime, preAuthorization.validFor)
                .formattedSpans(boldStyle = RadixTheme.typography.body2HighImportance.toSpanStyle()),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.pink1,
            modifier = Modifier.padding(
                horizontal = RadixTheme.dimensions.paddingDefault
            )
        )
    }
}

@Composable
private fun Info(
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.gray5,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingSemiLarge
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(id = R.string.preAuthorizationReview_fees_title),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )

            Text(
                text = stringResource(id = R.string.preAuthorizationReview_fees_subtitle),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXXLarge))

        Box(
            modifier = Modifier
                .padding(
                    start = RadixTheme.dimensions.paddingXXLarge,
                    end = RadixTheme.dimensions.paddingXSmall
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onInfoClick
                )
        ) {
            InfoButton(
                text = stringResource(id = R.string.empty),
                color = RadixTheme.colors.gray2,
                onClick = onInfoClick
            )
        }
    }
}

@Composable
@Preview
private fun TransactionPreAuthorizationInfoPreview() {
    RadixWalletPreviewTheme {
        TransactionPreAuthorizationInfo(
            preAuthorization = TransactionReviewViewModel.State.PreAuthorization(
                validFor = "23:03 minutes"
            ),
            onInfoClick = {}
        )
    }
}
