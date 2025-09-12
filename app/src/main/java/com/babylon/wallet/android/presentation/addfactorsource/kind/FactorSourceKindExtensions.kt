package com.babylon.wallet.android.presentation.addfactorsource.kind

import com.radixdlt.sargon.FactorSourceKind

private val supportedFactorSourceKinds = listOf(
    FactorSourceKind.DEVICE,
    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
    FactorSourceKind.ARCULUS_CARD,
    FactorSourceKind.OFF_DEVICE_MNEMONIC
)

val FactorSourceKind.isSupported
    get() = this in supportedFactorSourceKinds
