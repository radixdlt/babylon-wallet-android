package com.babylon.wallet.android.presentation.ui.model.factors

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R

sealed interface FactorSourceStatusMessage {

    data object PassphraseHint : FactorSourceStatusMessage

    data class Dynamic(
        val message: StatusMessage
    ) : FactorSourceStatusMessage

    @Composable
    fun getMessage(): StatusMessage = when (this) {
        is PassphraseHint -> StatusMessage(
            message = stringResource(id = R.string.shieldSetupPrepareFactors_addAnotherFactor_passphraseHint),
            type = StatusMessage.Type.WARNING
        )
        is Dynamic -> message
    }
}
