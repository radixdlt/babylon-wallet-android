package com.babylon.wallet.android.presentation.common.secured

import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard

sealed interface SecuredWithUiData {

    data object Shield : SecuredWithUiData

    data class Factor(
        val factorSourceCard: FactorSourceCard
    ) : SecuredWithUiData
}
