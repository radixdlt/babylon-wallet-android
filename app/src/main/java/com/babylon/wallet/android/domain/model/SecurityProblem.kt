package com.babylon.wallet.android.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R

sealed interface SecurityProblem {
    data class EntitiesNeedBackup(
        val factorSourceID: String,
        val accountsNeedBackup: Int,
        val personasNeedBackup: Int
    ) : SecurityProblem

    data class EntitiesNeedRecovery(val factorSourceID: String) : SecurityProblem

    data object BackupNotWorking : SecurityProblem
}

@Composable
fun SecurityProblem.toProblemHeading(): String {
    return when (this) {
        is SecurityProblem.EntitiesNeedBackup -> stringResource(
            id = R.string.securityCenter_problem3_heading,
            accountsNeedBackup,
            personasNeedBackup
        )

        is SecurityProblem.EntitiesNeedRecovery -> stringResource(id = R.string.securityCenter_problem9_heading)
        SecurityProblem.BackupNotWorking -> stringResource(id = R.string.securityCenter_problem6_heading)
    }
}
