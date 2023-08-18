package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun SimpleAccountCard(
    modifier: Modifier = Modifier,
    account: AccountItemUiModel,
    vertical: Boolean = false
) {
    if (vertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                modifier = Modifier.padding(end = RadixTheme.dimensions.paddingMedium),
                text = account.displayName.orEmpty(),
                style = RadixTheme.typography.body1Header,
                maxLines = 1,
                color = RadixTheme.colors.white,
                overflow = TextOverflow.Ellipsis
            )
            ActionableAddressView(
                address = account.address,
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
            )
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.padding(end = RadixTheme.dimensions.paddingMedium),
                text = account.displayName.orEmpty(),
                style = RadixTheme.typography.body1Header,
                maxLines = 1,
                color = RadixTheme.colors.white,
                overflow = TextOverflow.Ellipsis
            )
            ActionableAddressView(
                address = account.address,
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SimpleAccountCard(
    modifier: Modifier = Modifier,
    account: Network.Account
) {
    Row(
        modifier = modifier
            .background(
                Brush.horizontalGradient(AccountGradientList[account.appearanceID % AccountGradientList.size]),
                RadixTheme.shapes.roundedRectSmall
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier.padding(end = RadixTheme.dimensions.paddingMedium),
            text = account.displayName,
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            color = RadixTheme.colors.white,
            overflow = TextOverflow.Ellipsis
        )
        ActionableAddressView(
            address = account.address,
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
        )
    }
}
