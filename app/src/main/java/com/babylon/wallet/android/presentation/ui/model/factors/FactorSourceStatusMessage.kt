package com.babylon.wallet.android.presentation.ui.model.factors

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.model.shared.StatusMessage
import com.babylon.wallet.android.utils.formattedSpans

sealed interface FactorSourceStatusMessage {

    data object PassphraseHint : FactorSourceStatusMessage

    data object CannotBeUsedHere : FactorSourceStatusMessage

    data object NoSecurityIssues : FactorSourceStatusMessage

    sealed interface SecurityPrompt : FactorSourceStatusMessage {
        data object WriteDownSeedPhrase : SecurityPrompt
        data object LostFactorSource : SecurityPrompt
        data object EntitiesNotRecoverable : SecurityPrompt
        data object SeedPhraseNeedRecovery : SecurityPrompt
    }

    data class Dynamic(
        val message: StatusMessage
    ) : FactorSourceStatusMessage

    @Composable
    fun getMessage(): StatusMessage = when (this) {
        is PassphraseHint -> StatusMessage(
            message = stringResource(id = R.string.shieldSetupPrepareFactors_addAnotherFactor_offDeviceMnemonicHint),
            type = StatusMessage.Type.WARNING
        )
        CannotBeUsedHere -> StatusMessage(
            message = stringResource(id = R.string.securityFactors_selectFactor_disabled)
                .formattedSpans(
                    boldStyle = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = RadixTheme.colors.textButton
                    )
                ),
            type = StatusMessage.Type.WARNING
        )
        SecurityPrompt.LostFactorSource -> StatusMessage(
            message = stringResource(R.string.factorSources_list_lostFactorSource),
            type = StatusMessage.Type.ERROR
        )
        SecurityPrompt.WriteDownSeedPhrase -> StatusMessage(
            message = stringResource(R.string.factorSources_list_seedPhraseNotRecoverable),
            type = StatusMessage.Type.WARNING
        )
        SecurityPrompt.EntitiesNotRecoverable -> StatusMessage(
            message = stringResource(R.string.securityProblems_no3_securityFactors),
            type = StatusMessage.Type.WARNING
        )
        SecurityPrompt.SeedPhraseNeedRecovery -> StatusMessage(
            message = stringResource(id = R.string.securityProblems_no9_securityFactors),
            type = StatusMessage.Type.WARNING
        )
        NoSecurityIssues -> StatusMessage(
            message = stringResource(R.string.factorSources_list_seedPhraseWrittenDown),
            type = StatusMessage.Type.SUCCESS
        )
        is Dynamic -> message
    }
}
