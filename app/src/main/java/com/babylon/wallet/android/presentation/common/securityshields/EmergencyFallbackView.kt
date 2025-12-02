package com.babylon.wallet.android.presentation.common.securityshields

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.common.title
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.assets.assetOutlineBorder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.TimePeriod

@Composable
fun EmergencyFallbackView(
    modifier: Modifier = Modifier,
    delay: TimePeriod,
    description: AnnotatedString,
    onInfoClick: (GlossaryItem) -> Unit,
    timePeriodTitle: String? = null,
    note: String? = null,
    onNumberOfDaysClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .background(
                color = RadixTheme.colors.errorSecondary,
                shape = RadixTheme.shapes.roundedRectMedium
            ),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RadixTheme.colors.error,
                    shape = RadixTheme.shapes.roundedRectTopMedium
                )
                .clip(RadixTheme.shapes.roundedRectTopMedium)
                .clickable { onInfoClick(GlossaryItem.emergencyfallback) }
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.shieldWizardRecovery_fallback_title),
                style = RadixTheme.typography.body1Header,
                color = White
            )

            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(DSR.ic_info_outline),
                tint = White,
                contentDescription = "info"
            )
        }

        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = description,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text
        )

        timePeriodTitle?.let {
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = it,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.error
            )
        }

        Row(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
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
                )
                .throttleClickable { onNumberOfDaysClick() },
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

        note?.let {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                text = it,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.error
            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
    }
}
