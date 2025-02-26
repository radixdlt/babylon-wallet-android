package com.babylon.wallet.android.domain.usecases.factorsources

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.domain.model.FactorSourceCategory
import com.babylon.wallet.android.domain.model.FactorSourceKindsByCategory
import com.radixdlt.sargon.FactorSourceKind
import javax.inject.Inject

class GetFactorSourceKindsByCategoryUseCase @Inject constructor() {

    operator fun invoke(): List<FactorSourceKindsByCategory> = if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
        listOf(
            FactorSourceKindsByCategory(
                category = FactorSourceCategory.Identity,
                kinds = listOf(FactorSourceKind.DEVICE)
            ),
            FactorSourceKindsByCategory(
                category = FactorSourceCategory.Hardware,
                kinds = listOf(
                    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    FactorSourceKind.ARCULUS_CARD
                )
            ),
            FactorSourceKindsByCategory(
                category = FactorSourceCategory.Information,
                kinds = listOf(
                    FactorSourceKind.PASSWORD,
                    FactorSourceKind.OFF_DEVICE_MNEMONIC
                )
            )
        )
    } else {
        listOf(
            FactorSourceKindsByCategory(
                category = FactorSourceCategory.Identity,
                kinds = listOf(FactorSourceKind.DEVICE)
            ),
            FactorSourceKindsByCategory(
                category = FactorSourceCategory.Hardware,
                kinds = listOf(FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET)
            )
        )
    }
}
