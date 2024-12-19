package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.common

import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceInstanceCard
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind

fun FactorSource.toCompactInstanceCard(includeDescription: Boolean): FactorSourceInstanceCard = when (this) {
    is FactorSource.ArculusCard -> FactorSourceInstanceCard.compact(
        id = id,
        name = value.hint.label,
        kind = value.kind,
        includeDescription = includeDescription
    )
    is FactorSource.Device -> FactorSourceInstanceCard.compact(
        id = id,
        name = value.hint.deviceName,
        kind = value.kind,
        includeDescription = includeDescription
    )
    is FactorSource.Ledger -> FactorSourceInstanceCard.compact(
        id = id,
        name = value.hint.label,
        kind = value.kind,
        includeDescription = includeDescription
    )
    is FactorSource.OffDeviceMnemonic -> FactorSourceInstanceCard.compact(
        id = id,
        name = value.hint.label.value,
        kind = value.kind,
        includeDescription = true
    )
    is FactorSource.Password -> FactorSourceInstanceCard.compact(
        id = id,
        name = value.hint.label,
        kind = value.kind,
        includeDescription = true
    )
    is FactorSource.SecurityQuestions,
    is FactorSource.TrustedContact -> error("Not supported yet")
}
