package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.profile.data.model.pernetwork.Network

fun LazyListScope.feePayerSelectionContent(
    candidates: List<FeePayerSearchResult.FeePayerCandidate>,
    onPayerSelected: (Network.Account) -> Unit
) {
    item {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingXXLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                ),
            text = stringResource(id = R.string.transactionReview_selectFeePayer_navigationTitle),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
    items(candidates) { candidate ->
        val gradientColor = getAccountGradientColorsFor(candidate.account.appearanceID)
        AccountSelectionCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .background(
                    brush = Brush.horizontalGradient(gradientColor),
                    shape = RadixTheme.shapes.roundedRectMedium
                )
                .clip(RadixTheme.shapes.roundedRectMedium)
                .throttleClickable {
                    onPayerSelected(candidate.account)
                },
            accountName = candidate.account.displayName,
            address = candidate.account.address,
            checked = false,
            isSingleChoice = true,
            radioButtonClicked = {
                onPayerSelected(candidate.account)
            }
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
    }
}
