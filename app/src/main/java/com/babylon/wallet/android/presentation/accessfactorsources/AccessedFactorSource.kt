package com.babylon.wallet.android.presentation.accessfactorsources

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase

sealed interface AccessedFactorSource {
    data class Device(
        val factorSource: FactorSource.Device,
        val mnemonicWithPassphrase: MnemonicWithPassphrase
    ) : AccessedFactorSource

    data class Ledger(val factorSource: FactorSource.Ledger) : AccessedFactorSource

    data class ArculusCard(val factorSource: FactorSource.ArculusCard) : AccessedFactorSource

    data class OffDeviceMnemonic(val factorSource: FactorSource.OffDeviceMnemonic) : AccessedFactorSource

    data class Password(val factorSource: FactorSource.Password) : AccessedFactorSource
}

fun FactorSource.asAccessed(): AccessedFactorSource = when (this) {
    is FactorSource.Device -> error("A Device factor source must be accessed with its mnemonic")
    is FactorSource.Ledger -> AccessedFactorSource.Ledger(this)
    is FactorSource.ArculusCard -> AccessedFactorSource.ArculusCard(this)
    is FactorSource.OffDeviceMnemonic -> AccessedFactorSource.OffDeviceMnemonic(this)
    is FactorSource.Password -> AccessedFactorSource.Password(this)
}
