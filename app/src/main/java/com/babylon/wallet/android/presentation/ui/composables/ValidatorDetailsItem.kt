package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.Address
import rdx.works.core.domain.resources.Validator

@Composable
fun ValidatorDetailsItem(validator: Validator, modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Thumbnail.Validator(
            modifier = Modifier.size(iconSize),
            validator = validator
        )
        Column {
            Text(
                validator.name,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text,
                maxLines = 1
            )

            ActionableAddressView(
                address = Address.Validator(validator.address),
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = RadixTheme.colors.text
            )
        }
    }
}
