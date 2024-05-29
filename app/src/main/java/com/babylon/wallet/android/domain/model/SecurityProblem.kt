package com.babylon.wallet.android.domain.model

// refer to this table
// https://radixdlt.atlassian.net/wiki/spaces/AT/pages/3392569357/Security-related+Problem+States+in+the+Wallet
sealed interface SecurityProblem {
    data class EntitiesNotRecoverable(
        val accountsNeedBackup: Int,
        val personasNeedBackup: Int
    ) : SecurityProblem

    data class SeedPhraseNeedRecovery(val arePersonasAffected: Boolean) : SecurityProblem

    sealed interface BackupNotWorking : SecurityProblem {
        data object BackupServiceError : BackupNotWorking
        data class BackupDisabled(val hasManualBackup: Boolean) : BackupNotWorking
    }

    val hasBackupProblems: Boolean
        get() = when (this) {
            is BackupNotWorking.BackupDisabled -> true
            BackupNotWorking.BackupServiceError -> true
            is EntitiesNotRecoverable -> false
            is SeedPhraseNeedRecovery -> false
        }

    val isSecurityFactorRelated: Boolean
        get() = when (this) {
            is EntitiesNotRecoverable -> true
            is SeedPhraseNeedRecovery -> true
            is BackupNotWorking -> false
        }
}
