package com.babylon.wallet.android.presentation.settings.securitycenter.common.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.StatusMessageText
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.SecurityShieldBuilderInvalidReason

@Composable
fun ShieldSetupStatusView(
    modifier: Modifier = Modifier,
    status: SecurityShieldBuilderInvalidReason,
    onInfoClick: (GlossaryItem) -> Unit
) {
    when (status) {
        is SecurityShieldBuilderInvalidReason.PrimaryRoleMustHaveAtLeastOneFactor -> StatusMessageText(
            modifier = modifier,
            message = StatusMessage(
                message = "You need at least 1 factor to sign transactions. Select a signing factor to continue to the next step.", // TODO crowdin
                type = StatusMessage.Type.WARNING
            )
        )
        else -> StatusMessageText(
            modifier = modifier.noIndicationClickable { onInfoClick(GlossaryItem.buildingshield) },
            message = StatusMessage(
                // TODO crowdin
                message = "You cannot create a Shield with this combination of factors. **Read more**".formattedSpans(
                    boldStyle = SpanStyle(
                        color = RadixTheme.colors.blue2,
                        fontWeight = RadixTheme.typography.body1StandaloneLink.fontWeight,
                        fontSize = RadixTheme.typography.body2Link.fontSize
                    )
                ),
                type = StatusMessage.Type.ERROR
            )
        )
    }
}
