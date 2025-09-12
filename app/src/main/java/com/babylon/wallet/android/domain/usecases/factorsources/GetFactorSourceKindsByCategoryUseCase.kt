package com.babylon.wallet.android.domain.usecases.factorsources

import com.babylon.wallet.android.domain.model.FactorSourceCategory
import com.babylon.wallet.android.domain.model.FactorSourceKindsByCategory
import com.radixdlt.sargon.FactorSourceKind
import javax.inject.Inject

class GetFactorSourceKindsByCategoryUseCase @Inject constructor() {

    operator fun invoke(): List<FactorSourceKindsByCategory> = listOf(
        FactorSourceKindsByCategory(
            category = FactorSourceCategory.Identity,
            kinds = listOf(FactorSourceKind.DEVICE)
        ),
        FactorSourceKindsByCategory(
            category = FactorSourceCategory.Hardware,
            kinds = listOf(
                FactorSourceKind.ARCULUS_CARD,
                FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET
            )
        ),
        FactorSourceKindsByCategory(
            category = FactorSourceCategory.Information,
            kinds = listOf(
                FactorSourceKind.OFF_DEVICE_MNEMONIC
            )
        )
    )
}
