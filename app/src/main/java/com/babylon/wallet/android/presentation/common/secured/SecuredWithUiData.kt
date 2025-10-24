package com.babylon.wallet.android.presentation.common.secured

import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard

sealed interface SecuredWithUiData {

    data class Shield(
        val isInTimedRecovery: Boolean
    ) : SecuredWithUiData

    data class Factor(
        val factorSourceCard: FactorSourceCard
    ) : SecuredWithUiData
}
