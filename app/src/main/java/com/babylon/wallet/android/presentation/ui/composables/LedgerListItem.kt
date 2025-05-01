package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.utils.dayMonthDateShort
import com.radixdlt.sargon.FactorSource

@Composable
fun LedgerListItem(
    ledgerFactorSource: FactorSource.Ledger,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    onLedgerSelected: ((FactorSource.Ledger) -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ledgerFactorSource.value.hint.label,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.text
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            val usedText = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(id = R.string.ledgerHardwareDevices_usedHeading))
                }
                append(": ${ledgerFactorSource.value.common.lastUsedOn.toInstant().dayMonthDateShort()}")
            }
            val addedText = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(id = R.string.ledgerHardwareDevices_addedHeading))
                }
                append(": ${ledgerFactorSource.value.common.addedOn.toInstant().dayMonthDateShort()}")
            }
            Text(
                text = usedText,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary
            )
            Text(
                text = addedText,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary
            )
        }
        if (selected != null) {
            RadixRadioButton(
                selected = selected,
                colors = RadixRadioButtonDefaults.darkColors(),
                onClick = {
                    onLedgerSelected?.invoke(ledgerFactorSource)
                },
            )
        }
    }
}
