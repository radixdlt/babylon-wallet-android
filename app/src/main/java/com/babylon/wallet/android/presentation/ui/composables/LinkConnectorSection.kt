package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun LinkConnectorSection(modifier: Modifier, onAddP2PLink: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painterResource(id = R.drawable.ic_radix_connect),
            tint = Color.Unspecified,
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        Text(
            text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_message),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            onClick = onAddP2PLink,
            text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_continue)
        )
    }
}
