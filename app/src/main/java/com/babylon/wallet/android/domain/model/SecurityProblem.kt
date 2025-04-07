package com.babylon.wallet.android.domain.model

// refer to this table
// https://radixdlt.atlassian.net/wiki/spaces/AT/pages/3392569357/Security-related+Problem+States+in+the+Wallet
sealed interface SecurityProblem {

    // security problem 3
    data class EntitiesNotRecoverable(
        val accountsNeedBackup: Int,
        val personasNeedBackup: Int,
        val hiddenAccountsNeedBackup: Int,
        val hiddenPersonasNeedBackup: Int
    ) : SecurityProblem

    sealed interface CloudBackupNotWorking : SecurityProblem {
        // security problem 5
        data class ServiceError(val isAnyActivePersonaAffected: Boolean) : CloudBackupNotWorking

        // security problem 6 & 7
        data class Disabled(
            val isAnyActivePersonaAffected: Boolean,
            val hasManualBackup: Boolean
        ) : CloudBackupNotWorking
    }

    // security problem 9
    data class SeedPhraseNeedRecovery(val isAnyActivePersonaAffected: Boolean) : SecurityProblem

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

fun com.radixdlt.sargon.SecurityProblem.toDomainModel(
    isAnyActivePersonaAffected: Boolean,
    hasManualBackup: Boolean
): SecurityProblem {
    return when (this) {
        is com.radixdlt.sargon.SecurityProblem.Problem3 -> {
            SecurityProblem.EntitiesNotRecoverable(
                accountsNeedBackup = this.addresses.accounts.count(),
                personasNeedBackup = this.addresses.personas.count(),
                hiddenAccountsNeedBackup = this.addresses.hiddenAccounts.count(),
                hiddenPersonasNeedBackup = this.addresses.hiddenPersonas.count()
            )
        }
        com.radixdlt.sargon.SecurityProblem.Problem5 -> {
            SecurityProblem.CloudBackupNotWorking.ServiceError(
                isAnyActivePersonaAffected = isAnyActivePersonaAffected
            )
        }
        com.radixdlt.sargon.SecurityProblem.Problem6 -> {
            SecurityProblem.CloudBackupNotWorking.Disabled(
                isAnyActivePersonaAffected = isAnyActivePersonaAffected,
                hasManualBackup = hasManualBackup
            )
        }
        com.radixdlt.sargon.SecurityProblem.Problem7 -> {
            SecurityProblem.CloudBackupNotWorking.Disabled(
                isAnyActivePersonaAffected = isAnyActivePersonaAffected,
                hasManualBackup = hasManualBackup
            )
        }
        is com.radixdlt.sargon.SecurityProblem.Problem9 -> {
            SecurityProblem.SeedPhraseNeedRecovery(
                isAnyActivePersonaAffected = this.addresses.personas.isNotEmpty()
            )
        }
    }
}
