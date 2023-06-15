package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.utils.ledgerLastUsedDateFormat
import rdx.works.profile.data.model.factorsources.FactorSource

@Composable
fun LedgerListItem(
    ledgerFactorSource: FactorSource,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    onLedgerSelected: ((FactorSource) -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ledgerFactorSource.label,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            val usedText = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(id = R.string.ledgerHardwareDevices_usedHeading))
                }
                append(": " + ledgerFactorSource.lastUsedOn.ledgerLastUsedDateFormat())
            }
            val addedText = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(id = R.string.ledgerHardwareDevices_addedHeading))
                }
                append(": " + ledgerFactorSource.addedOn.ledgerLastUsedDateFormat())
            }
            Text(
                text = addedText,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            Text(
                text = usedText,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
        if (selected != null) {
            RadioButton(
                selected = selected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = RadixTheme.colors.gray1,
                    unselectedColor = RadixTheme.colors.gray3,
                    disabledSelectedColor = Color.White
                ),
                onClick = {
                    onLedgerSelected?.invoke(ledgerFactorSource)
                },
            )
        }
    }
}
