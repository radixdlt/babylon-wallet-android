package com.babylon.wallet.android.presentation.ui.model.securityshields

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.model.shared.StatusMessage

sealed interface SecurityShieldStatusMessage {

    data object AppliedAndWorking : SecurityShieldStatusMessage

    data object ActionRequired : SecurityShieldStatusMessage

    @Composable
    fun getMessage(): StatusMessage = when (this) {
        AppliedAndWorking -> StatusMessage(
            message = stringResource(id = R.string.securityShields_status_applied),
            type = StatusMessage.Type.SUCCESS
        )

        ActionRequired -> StatusMessage(
            message = stringResource(id = R.string.securityShields_status_actionRequired),
            type = StatusMessage.Type.WARNING
        )
    }
}
