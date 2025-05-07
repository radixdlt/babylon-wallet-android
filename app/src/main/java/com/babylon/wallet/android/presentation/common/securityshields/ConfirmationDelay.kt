package com.babylon.wallet.android.presentation.common.securityshields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.title
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.assets.assetOutlineBorder
import com.radixdlt.sargon.TimePeriod

@Composable
fun ConfirmationDelay(
    modifier: Modifier = Modifier,
    delay: TimePeriod
) {
    Column(
        modifier = modifier
            .background(
                color = RadixTheme.colors.errorSecondary,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Text(
            text = stringResource(R.string.transactionReview_updateShield_confirmationDelayMessage),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RadixTheme.colors.background,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
                .assetOutlineBorder(
                    shape = RadixTheme.shapes.roundedRectSmall
                )
                .padding(
                    horizontal = RadixTheme.dimensions.paddingSemiLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_calendar),
                contentDescription = null,
                tint = RadixTheme.colors.icon
            )

            Text(
                text = delay.title(),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text
            )
        }
    }
}
