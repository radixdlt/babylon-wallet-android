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
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.profile.data.model.pernetwork.Network

fun LazyListScope.feePayerSelectionContent(
    candidates: List<Network.Account>,
    onPayerSelected: (Network.Account) -> Unit
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
    }
    items(candidates) { candidate ->
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
            checked = false,
            isSingleChoice = true,
            radioButtonClicked = {
                onPayerSelected(candidate)
            }
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
    }
}
