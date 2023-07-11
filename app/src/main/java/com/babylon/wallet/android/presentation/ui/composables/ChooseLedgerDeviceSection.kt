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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource

@Composable
fun ChooseLedgerDeviceSection(
    modifier: Modifier,
    ledgerFactorSources: ImmutableList<Selectable<LedgerHardwareWalletFactorSource>>,
    onAddLedger: () -> Unit,
    onLedgerFactorSourceSelected: (LedgerHardwareWalletFactorSource) -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painterResource(id = R.drawable.ic_hardware_ledger),
            tint = Color.Unspecified,
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            text = "Choose Ledger Device",
            // todo stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_navigationTitleAllowSelection),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            text = "Choose an existing Ledger or add a new one",
            // todo stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_subtitleSelectLedger),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        if (ledgerFactorSources.isEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectSmall)
                    .padding(RadixTheme.dimensions.paddingLarge),
                text = "No Ledger devices currently added to your Radix Wallet",
                // todo stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_subtitleNoLedgers),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            RadixPrimaryButton(
                text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_addNewLedger),
                onClick = onAddLedger,
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
                    items = ledgerFactorSources,
                    key = { item ->
                        item.data.id.body.value
                    },
                    itemContent = { ledgerItem ->
                        LedgerListItem(
                            ledgerFactorSource = ledgerItem.data,
                            modifier = Modifier
                                .shadow(elevation = 4.dp, shape = RadixTheme.shapes.roundedRectSmall)
                                .fillMaxWidth()
                                .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectSmall)
                                .throttleClickable {
                                    onLedgerFactorSourceSelected(ledgerItem.data)
                                }
                                .padding(RadixTheme.dimensions.paddingLarge),
                            selected = ledgerItem.selected,
                            onLedgerSelected = onLedgerFactorSourceSelected
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                    }
                )
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .imePadding(),
                        onClick = onAddLedger,
                        text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_addNewLedger)
                    )
                }
            }
        }
    }
}
