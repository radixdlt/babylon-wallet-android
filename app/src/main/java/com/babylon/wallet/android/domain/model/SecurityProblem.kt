package com.babylon.wallet.android.domain.model

sealed interface SecurityProblem {
    data class EntitiesNotRecoverable(
        val accountsNeedBackup: Int,
        val personasNeedBackup: Int
    ) : SecurityProblem

    data class SeedPhraseNeedRecovery(val arePersonasAffected: Boolean) : SecurityProblem

    data object BackupNotWorking : SecurityProblem
}
