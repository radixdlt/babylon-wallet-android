package com.babylon.wallet.android.presentation.accessfactorsources.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun RoundLedgerItem(
    modifier: Modifier = Modifier,
    ledgerName: String
) {
    Row(
        modifier = modifier
            .background(RadixTheme.colors.gray5, RadixTheme.shapes.circle)
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            painter = painterResource(
                id = R.drawable.ic_security_key
            ),
            contentDescription = null,
            tint = RadixTheme.colors.gray3 // TODO Theme
        )
        Text(
            text = ledgerName,
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.text
        )
    }
}
