package com.babylon.wallet.android.presentation.discover.common.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.DSR
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.dialogs.info.applyIconTint
import com.babylon.wallet.android.presentation.dialogs.info.resolveIconFromGlossaryItem

@Composable
fun InfoGlossaryItemView(
    item: GlossaryItem,
    onClick: (GlossaryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    SimpleListItemView(
        modifier = modifier,
        leadingIcon = {
            val itemIcon = item.resolveIconFromGlossaryItem()
            Image(
                modifier = Modifier.size(48.dp),
                painter = painterResource(itemIcon ?: DSR.ic_info_outline),
                contentDescription = null,
                colorFilter = if (item.applyIconTint) {
                    ColorFilter.tint(RadixTheme.colors.icon)
                } else {
                    null
                }
            )
        },
        title = item.resolveTitleFromGlossaryItem(),
        description = item.resolveDescriptionFromGlossaryItem(),
        onClick = { onClick(item) }
    )
}

@Composable
private fun GlossaryItem.resolveTitleFromGlossaryItem() =
    stringResource(resolveTitleResFromGlossaryItem())

@Composable
private fun GlossaryItem.resolveDescriptionFromGlossaryItem() =
    stringResource(resolveDescriptionResFromGlossaryItem())

@Suppress("CyclomaticComplexMethod")
fun GlossaryItem.resolveTitleResFromGlossaryItem() = when (this) {
    GlossaryItem.radixnetwork -> R.string.infoLink_discoverTitle_radixnetwork
    GlossaryItem.nfts -> R.string.infoLink_discoverTitle_nfts
    GlossaryItem.guarantees -> R.string.infoLink_discoverTitle_guarantees
    GlossaryItem.web3 -> R.string.infoLink_discoverTitle_web3
    GlossaryItem.radixwallet -> R.string.infoLink_discoverTitle_radixwallet
    GlossaryItem.radixconnect -> R.string.infoLink_discoverTitle_radixconnect
    GlossaryItem.radixconnector -> R.string.infoLink_discoverTitle_radixconnector
    GlossaryItem.xrd -> R.string.infoLink_discoverTitle_xrd
    GlossaryItem.dashboard -> R.string.infoLink_discoverTitle_dashboard
    GlossaryItem.dapps -> R.string.infoLink_discoverTitle_dapps
    GlossaryItem.connectbutton -> R.string.infoLink_discoverTitle_connectbutton
    GlossaryItem.dex -> R.string.infoLink_discoverTitle_dex
    GlossaryItem.accounts -> R.string.infoLink_discoverTitle_accounts
    GlossaryItem.personas -> R.string.infoLink_discoverTitle_personas
    GlossaryItem.tokens -> R.string.infoLink_discoverTitle_tokens
    GlossaryItem.claimnfts -> R.string.infoLink_discoverTitle_claimnfts
    GlossaryItem.networkstaking -> R.string.infoLink_discoverTitle_networkstaking
    GlossaryItem.poolunits -> R.string.infoLink_discoverTitle_poolunits
    GlossaryItem.liquidstakeunits -> R.string.infoLink_discoverTitle_liquidstakeunits
    GlossaryItem.badges -> R.string.infoLink_discoverTitle_badges
    GlossaryItem.behaviors -> R.string.infoLink_discoverTitle_behaviors
    GlossaryItem.transfers -> R.string.infoLink_discoverTitle_transfers
    GlossaryItem.transactions -> R.string.infoLink_discoverTitle_transactions
    GlossaryItem.transactionfee -> R.string.infoLink_discoverTitle_transactionfee
    GlossaryItem.payingaccount -> R.string.infoLink_discoverTitle_payingaccount
    GlossaryItem.validators -> R.string.infoLink_discoverTitle_validators
    GlossaryItem.bridging -> R.string.infoLink_discoverTitle_bridging
    GlossaryItem.gateways -> R.string.infoLink_discoverTitle_gateways
    GlossaryItem.preauthorizations -> R.string.infoLink_discoverTitle_preauthorizations
    GlossaryItem.possibledappcalls -> R.string.infoLink_discoverTitle_possibledappcalls
    GlossaryItem.biometricspin -> R.string.infoLink_discoverTitle_biometricspin
    GlossaryItem.ledgernano -> R.string.infoLink_discoverTitle_ledgernano
    GlossaryItem.securityshields -> R.string.infoLink_discoverTitle_securityshields
    GlossaryItem.buildingshield -> R.string.infoLink_discoverTitle_buildingshield
    GlossaryItem.emergencyfallback -> R.string.infoLink_discoverTitle_emergencyfallback
    GlossaryItem.arculus -> R.string.infoLink_discoverTitle_arculus
    GlossaryItem.passwords -> R.string.infoLink_discoverTitle_passwords
    GlossaryItem.mnemonics -> R.string.infoLink_discoverTitle_passphrases
}

@Suppress("CyclomaticComplexMethod")
fun GlossaryItem.resolveDescriptionResFromGlossaryItem() = when (this) {
    GlossaryItem.radixnetwork -> R.string.infoLink_discoverDescription_radixnetwork
    GlossaryItem.nfts -> R.string.infoLink_discoverDescription_nfts
    GlossaryItem.guarantees -> R.string.infoLink_discoverDescription_guarantees
    GlossaryItem.web3 -> R.string.infoLink_discoverDescription_web3
    GlossaryItem.radixwallet -> R.string.infoLink_discoverDescription_radixwallet
    GlossaryItem.radixconnect -> R.string.infoLink_discoverDescription_radixconnect
    GlossaryItem.radixconnector -> R.string.infoLink_discoverDescription_radixconnector
    GlossaryItem.xrd -> R.string.infoLink_discoverDescription_xrd
    GlossaryItem.dashboard -> R.string.infoLink_discoverDescription_dashboard
    GlossaryItem.dapps -> R.string.infoLink_discoverDescription_dapps
    GlossaryItem.connectbutton -> R.string.infoLink_discoverDescription_connectbutton
    GlossaryItem.dex -> R.string.infoLink_discoverDescription_dex
    GlossaryItem.accounts -> R.string.infoLink_discoverDescription_accounts
    GlossaryItem.personas -> R.string.infoLink_discoverDescription_personas
    GlossaryItem.tokens -> R.string.infoLink_discoverDescription_tokens
    GlossaryItem.claimnfts -> R.string.infoLink_discoverDescription_claimnfts
    GlossaryItem.networkstaking -> R.string.infoLink_discoverDescription_networkstaking
    GlossaryItem.poolunits -> R.string.infoLink_discoverDescription_poolunits
    GlossaryItem.liquidstakeunits -> R.string.infoLink_discoverDescription_liquidstakeunits
    GlossaryItem.badges -> R.string.infoLink_discoverDescription_badges
    GlossaryItem.behaviors -> R.string.infoLink_discoverDescription_behaviors
    GlossaryItem.transfers -> R.string.infoLink_discoverDescription_transfers
    GlossaryItem.transactions -> R.string.infoLink_discoverDescription_transactions
    GlossaryItem.transactionfee -> R.string.infoLink_discoverDescription_transactionfee
    GlossaryItem.payingaccount -> R.string.infoLink_discoverDescription_payingaccount
    GlossaryItem.validators -> R.string.infoLink_discoverDescription_validators
    GlossaryItem.bridging -> R.string.infoLink_discoverDescription_bridging
    GlossaryItem.gateways -> R.string.infoLink_discoverDescription_gateways
    GlossaryItem.preauthorizations -> R.string.infoLink_discoverDescription_preauthorizations
    GlossaryItem.possibledappcalls -> R.string.infoLink_discoverDescription_possibledappcalls
    GlossaryItem.biometricspin -> R.string.infoLink_discoverDescription_biometricspin
    GlossaryItem.ledgernano -> R.string.infoLink_discoverDescription_ledgernano
    GlossaryItem.securityshields -> R.string.infoLink_discoverDescription_securityshields
    GlossaryItem.buildingshield -> R.string.infoLink_discoverDescription_buildingshield
    GlossaryItem.emergencyfallback -> R.string.infoLink_discoverDescription_emergencyfallback
    GlossaryItem.arculus -> R.string.infoLink_discoverDescription_arculus
    GlossaryItem.passwords -> R.string.infoLink_discoverDescription_passwords
    GlossaryItem.mnemonics -> R.string.infoLink_discoverDescription_passphrases
}
