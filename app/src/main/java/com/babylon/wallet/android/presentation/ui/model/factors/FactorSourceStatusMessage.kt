package com.babylon.wallet.android.presentation.ui.model.factors

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R

sealed interface FactorSourceStatusMessage {

    data object PassphraseHint : FactorSourceStatusMessage

    data object NoSecurityIssues : FactorSourceStatusMessage

    sealed interface SecurityPrompt : FactorSourceStatusMessage {
        data object WriteDownSeedPhrase : SecurityPrompt
        data object RecoveryRequired : SecurityPrompt
    }

    data class Dynamic(
        val message: StatusMessage
    ) : FactorSourceStatusMessage

    @Composable
    fun getMessage(): StatusMessage = when (this) {
        is PassphraseHint -> StatusMessage(
            message = stringResource(id = R.string.shieldSetupPrepareFactors_addAnotherFactor_passphraseHint),
            type = StatusMessage.Type.WARNING
        )
        SecurityPrompt.RecoveryRequired -> StatusMessage(
            message = stringResource(R.string.factorSources_list_lostFactorSource),
            type = StatusMessage.Type.ERROR
        )
        SecurityPrompt.WriteDownSeedPhrase -> StatusMessage(
            message = stringResource(R.string.factorSources_list_seedPhraseNotRecoverable),
            type = StatusMessage.Type.WARNING
        )
        NoSecurityIssues -> StatusMessage(
            message = stringResource(R.string.factorSources_list_seedPhraseWrittenDown),
            type = StatusMessage.Type.SUCCESS
        )
        is Dynamic -> message
    }
}
