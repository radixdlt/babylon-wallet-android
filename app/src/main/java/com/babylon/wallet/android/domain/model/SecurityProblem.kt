package com.babylon.wallet.android.domain.model

// refer to this table
// https://radixdlt.atlassian.net/wiki/spaces/AT/pages/3392569357/Security-related+Problem+States+in+the+Wallet
sealed interface SecurityProblem {

    data class EntitiesNotRecoverable(
        val accountsNeedBackup: Int,
        val personasNeedBackup: Int,
        val hiddenAccountsNeedBackup: Int,
        val hiddenPersonasNeedBackup: Int
    ) : SecurityProblem

    data class SeedPhraseNeedRecovery(val isAnyActivePersonaAffected: Boolean) : SecurityProblem

    sealed interface CloudBackupNotWorking : SecurityProblem {
        data class ServiceError(val isAnyActivePersonaAffected: Boolean) : CloudBackupNotWorking
        data class Disabled(
            val isAnyActivePersonaAffected: Boolean,
            val hasManualBackup: Boolean
        ) : CloudBackupNotWorking
    }

    val hasCloudBackupProblems: Boolean
        get() = when (this) {
            is CloudBackupNotWorking.Disabled -> true
            is CloudBackupNotWorking.ServiceError -> true
            is EntitiesNotRecoverable -> false
            is SeedPhraseNeedRecovery -> false
        }

    val isSecurityFactorRelated: Boolean
        get() = when (this) {
            is EntitiesNotRecoverable -> true
            is SeedPhraseNeedRecovery -> true
            is CloudBackupNotWorking -> false
        }
}
