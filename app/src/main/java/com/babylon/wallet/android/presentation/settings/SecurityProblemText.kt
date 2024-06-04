package com.babylon.wallet.android.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.SecurityProblem

@Composable
fun SecurityProblem.toProblemHeading(): String {
    return when (this) {
        is SecurityProblem.CloudBackupNotWorking.Disabled -> {
            if (this.hasManualBackup) {
                stringResource(id = R.string.securityProblems_no7_securityCenterTitle)
            } else {
                stringResource(id = R.string.securityProblems_no6_securityCenterTitle)
            }
        }
        is SecurityProblem.CloudBackupNotWorking.ServiceError -> stringResource(id = R.string.securityProblems_no5_securityCenterTitle)
        is SecurityProblem.EntitiesNotRecoverable -> {
            val accountsString = if (accountsNeedBackup == 1) {
                stringResource(id = R.string.securityProblems_common_accountSingular)
            } else {
                stringResource(id = R.string.securityProblems_common_accountPlural, accountsNeedBackup)
            }
            val personasString = if (personasNeedBackup == 1) {
                stringResource(id = R.string.securityProblems_common_personaSingular)
            } else {
                stringResource(id = R.string.securityProblems_common_personaPlural, personasNeedBackup)
            }
            val accountsAndPersonasString = stringResource(
                id = R.string.securityProblems_no3_securityCenterTitle,
                accountsString,
                personasString
            )
            // TODO when we have crowdin
//            val hiddenEntitiesString = if (hiddenAccountsNeedBackup > 0 || hiddenPersonasNeedBackup > 0) {
//                " and hidden entities"
//            } else {
//                stringResource(R.string.empty)
//            }
//            accountsAndPersonasString + hiddenEntitiesString
            accountsAndPersonasString
        }

        is SecurityProblem.SeedPhraseNeedRecovery -> stringResource(id = R.string.securityProblems_no9_securityCenterTitle)
    }
}

@Composable
fun SecurityProblem.CloudBackupNotWorking.toText() = when (this) {
    is SecurityProblem.CloudBackupNotWorking.ServiceError -> {
        if (this.isAnyActivePersonaAffected) {
            stringResource(R.string.securityProblems_no5_walletSettingsPersonas)
        } else {
            null
        }
    }
    is SecurityProblem.CloudBackupNotWorking.Disabled -> {
        if (this.isAnyActivePersonaAffected) {
            if (this.hasManualBackup) {
                stringResource(R.string.securityProblems_no7_walletSettingsPersonas)
            } else {
                stringResource(R.string.securityProblems_no6_walletSettingsPersonas)
            }
        } else {
            null
        }
    }
}
