package com.babylon.wallet.android.presentation.settings.securitycenter.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.radixdlt.sargon.FactorSourceKind

@Composable
fun FactorSourceKind.infoButtonTitle() = stringResource(
    id = remember(this) {
        when (this) {
            FactorSourceKind.DEVICE -> R.string.infoLink_title_biometricspin
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> R.string.infoLink_title_ledgernano
            FactorSourceKind.OFF_DEVICE_MNEMONIC -> R.string.infoLink_title_passphrases
            FactorSourceKind.ARCULUS_CARD -> R.string.infoLink_title_arculus
            FactorSourceKind.PASSWORD -> R.string.infoLink_title_passwords
        }
    }
)

fun FactorSourceKind.infoGlossaryItem() = when (this) {
    FactorSourceKind.DEVICE -> GlossaryItem.biometricspin
    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> GlossaryItem.ledgernano
    FactorSourceKind.OFF_DEVICE_MNEMONIC -> GlossaryItem.mnemonics
    FactorSourceKind.ARCULUS_CARD -> GlossaryItem.arculus
    FactorSourceKind.PASSWORD -> GlossaryItem.passwords
}
