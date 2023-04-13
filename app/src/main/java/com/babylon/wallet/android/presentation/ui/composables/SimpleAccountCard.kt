package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel

@Composable
fun SimpleAccountCard(
    modifier: Modifier = Modifier,
    account: AccountItemUiModel
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = account.displayName.orEmpty(),
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            color = RadixTheme.colors.white,
            overflow = TextOverflow.Ellipsis
        )
        ActionableAddressView(
            modifier = Modifier.weight(1f),
            address = account.address,
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
        )
    }
}