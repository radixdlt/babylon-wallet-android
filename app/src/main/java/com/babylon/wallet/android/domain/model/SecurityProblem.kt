package com.babylon.wallet.android.domain.model

import com.radixdlt.sargon.FactorSourceId

sealed interface SecurityProblem {
    data class EntitiesNotRecoverable(
        val accountsNeedBackup: Int,
        val personasNeedBackup: Int
    ) : SecurityProblem

    data class EntitiesNeedRecovery(val factorSourceID: FactorSourceId) : SecurityProblem

    data object BackupNotWorking : SecurityProblem
}
