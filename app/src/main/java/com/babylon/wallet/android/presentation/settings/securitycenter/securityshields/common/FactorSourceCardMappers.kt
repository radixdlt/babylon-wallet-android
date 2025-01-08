package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.common

import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind

fun FactorSource.toCompactInstanceCard(includeDescription: Boolean): FactorSourceCard = when (this) {
    is FactorSource.ArculusCard -> FactorSourceCard.compact(
        id = id,
        name = value.hint.label,
        kind = value.kind,
        includeDescription = includeDescription
    )
    is FactorSource.Device -> FactorSourceCard.compact(
        id = id,
        name = value.hint.deviceName,
        kind = value.kind,
        includeDescription = includeDescription
    )
    is FactorSource.Ledger -> FactorSourceCard.compact(
        id = id,
        name = value.hint.label,
        kind = value.kind,
        includeDescription = includeDescription
    )
    is FactorSource.OffDeviceMnemonic -> FactorSourceCard.compact(
        id = id,
        name = value.hint.label.value,
        kind = value.kind,
        includeDescription = includeDescription
    )
    is FactorSource.Password -> FactorSourceCard.compact(
        id = id,
        name = value.hint.label,
        kind = value.kind,
        includeDescription = includeDescription
    )
    is FactorSource.SecurityQuestions,
    is FactorSource.TrustedContact -> error("Not supported yet")
}
