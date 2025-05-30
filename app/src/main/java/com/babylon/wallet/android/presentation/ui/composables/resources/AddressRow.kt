package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.Address

@Composable
fun AddressRow(
    modifier: Modifier = Modifier,
    label: String = stringResource(id = R.string.assetDetails_resourceAddress),
    address: Address,
    isNewlyCreatedEntity: Boolean = false
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.textSecondary
        )

        ActionableAddressView(
            address = address,
            isVisitableInDashboard = !isNewlyCreatedEntity,
            textStyle = RadixTheme.typography.body1HighImportance,
            textColor = RadixTheme.colors.text,
            iconColor = RadixTheme.colors.iconSecondary
        )
    }
}
