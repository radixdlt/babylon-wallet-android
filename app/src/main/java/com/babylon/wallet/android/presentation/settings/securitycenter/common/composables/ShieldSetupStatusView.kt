package com.babylon.wallet.android.presentation.settings.securitycenter.common.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.StatusMessageText
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage
import com.radixdlt.sargon.SecurityShieldBuilderInvalidReason

@Composable
fun ShieldSetupStatusView(
    modifier: Modifier = Modifier,
    status: SecurityShieldBuilderInvalidReason,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val readMoreGlossaryItem = GlossaryItem.buildingshield
    val message = when (status) {
        is SecurityShieldBuilderInvalidReason.PrimaryRoleMustHaveAtLeastOneFactor -> StatusMessage(
            message = "You need at least 1 factor to sign transactions. Select a signing factor to continue to the next step.", // TODO crowdin
            type = StatusMessage.Type.WARNING
        )
        else -> StatusMessage(
            message = buildStatusMessageAnnotatedString(
                message = "You cannot create a Shield with this combination of factors.", // TODO crowdin
                glossaryItem = readMoreGlossaryItem,
                annotation = "Read more" // TODO crowdin
            ),
            type = StatusMessage.Type.ERROR
        )
    }

    StatusMessageText(
        modifier = modifier,
        message = message,
        onTextClick = { offset -> message.onStatusMessageInfoAnnotationClick(offset, readMoreGlossaryItem, onInfoClick) }
    )
}
