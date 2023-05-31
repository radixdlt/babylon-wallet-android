package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.addedOnTimestampFormatted
import kotlinx.collections.immutable.ImmutableList
import rdx.works.profile.data.model.factorsources.FactorSource

@Composable
fun LedgerSelector(
    modifier: Modifier,
    selectedLedgerFactorSourceID: FactorSource.ID?,
    ledgerFactorSources: ImmutableList<FactorSource>,
    onLedgerFactorSourceSelected: (FactorSource) -> Unit
) {
    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .fillMaxWidth()
                .border(1.dp, RadixTheme.colors.gray1, shape = RadixTheme.shapes.roundedRectMedium)
                .clip(RadixTheme.shapes.roundedRectMedium)
                .throttleClickable { dropdownMenuExpanded = !dropdownMenuExpanded }
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedFactorSource = ledgerFactorSources.firstOrNull { it.id == selectedLedgerFactorSourceID }
            val selectedLedgerDescription = stringResource(
                id = com.babylon.wallet.android.R.string.selected_ledger_description,
                selectedFactorSource?.label.orEmpty(),
                selectedFactorSource?.addedOnTimestampFormatted().orEmpty()
            )
            Text(
                modifier = Modifier.weight(1f),
                text = selectedLedgerDescription,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Icon(painter = painterResource(id = R.drawable.ic_arrow_down), contentDescription = null, tint = RadixTheme.colors.gray1)
        }
        DropdownMenu(
            modifier = Modifier
                .background(RadixTheme.colors.defaultBackground)
                .height(200.dp),
            expanded = dropdownMenuExpanded,
            onDismissRequest = { dropdownMenuExpanded = false }
        ) {
            ledgerFactorSources.forEach { factorSource ->
                DropdownMenuItem(onClick = {
                    onLedgerFactorSourceSelected(factorSource)
                    dropdownMenuExpanded = false
                }) {
                    Column {
                        Text(
                            factorSource.label,
                            textAlign = TextAlign.Start,
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray1
                        )
                    }
                }
            }
        }
    }
}
