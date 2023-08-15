package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView

@Composable
fun ResourceAddressRow(
    modifier: Modifier,
    address: String
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.assetDetails_resourceAddress),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )

        ActionableAddressView(
            address = address,
            textStyle = RadixTheme.typography.body1HighImportance,
            textColor = RadixTheme.colors.gray1,
            iconColor = RadixTheme.colors.gray2
        )
    }
}
