package com.babylon.wallet.android.presentation.common.secured

import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.shared.TimedRecoveryDisplayData

sealed interface SecuredWithUiData {

    data class Shield(
        val timedRecovery: TimedRecoveryDisplayData?
    ) : SecuredWithUiData

    data class Factor(
        val factorSourceCard: FactorSourceCard
    ) : SecuredWithUiData
}
