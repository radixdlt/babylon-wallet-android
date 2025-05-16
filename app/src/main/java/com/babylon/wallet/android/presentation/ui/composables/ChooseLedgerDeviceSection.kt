package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.themedColorTint
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.hex
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import rdx.works.core.sargon.sample

@Composable
fun ChooseLedgerDeviceSection(
    modifier: Modifier,
    ledgerDevices: ImmutableList<Selectable<FactorSource.Ledger>>,
    onAddLedgerDeviceClick: () -> Unit,
    onLedgerDeviceSelected: (FactorSource.Ledger) -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painterResource(id = R.drawable.ic_hardware_ledger),
            tint = themedColorTint(),
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            text = if (ledgerDevices.isEmpty()) {
                stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_navigationTitleNoSelection)
            } else {
                stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_navigationTitleAllowSelection)
            },
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            text = if (ledgerDevices.isEmpty()) {
                stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_subtitleSelectLedgerExisting)
            } else {
                stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_subtitleSelectLedger)
            },
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        if (ledgerDevices.isEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.card, RadixTheme.shapes.roundedRectSmall)
                    .padding(RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_subtitleNoLedgers),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.textSecondary,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            RadixPrimaryButton(
                text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_addNewLedger),
                onClick = onAddLedgerDeviceClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .imePadding()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(
                    items = ledgerDevices,
                    key = { item ->
                        item.data.value.id.body.hex
                    },
                    itemContent = { ledgerItem ->
                        LedgerListItem(
                            ledgerFactorSource = ledgerItem.data,
                            modifier = Modifier
                                .shadow(elevation = 4.dp, shape = RadixTheme.shapes.roundedRectSmall)
                                .fillMaxWidth()
                                .background(RadixTheme.colors.card, shape = RadixTheme.shapes.roundedRectSmall)
                                .throttleClickable {
                                    onLedgerDeviceSelected(ledgerItem.data)
                                }
                                .padding(RadixTheme.dimensions.paddingLarge),
                            selected = ledgerItem.selected,
                            onLedgerSelected = onLedgerDeviceSelected
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                    }
                )
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .imePadding(),
                        onClick = onAddLedgerDeviceClick,
                        text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_addNewLedger)
                    )
                }
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun ChooseLedgerDeviceSectionPreview() {
    RadixWalletPreviewTheme {
        ChooseLedgerDeviceSection(
            modifier = Modifier,
            ledgerDevices = FactorSource.Ledger.sample.all
                .map {
                    Selectable(it)
                }.toImmutableList(),
            onAddLedgerDeviceClick = {},
            onLedgerDeviceSelected = {}
        )
    }
}
