package com.babylon.wallet.android.presentation.settings.securitycenter.common.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.StatusMessageText
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.SecurityShieldBuilderStatus

@Composable
fun ShieldSetupMissingFactorStatusView(
    modifier: Modifier = Modifier,
) {
    StatusMessageText(
        modifier = modifier,
        message = StatusMessage(
            message = stringResource(id = R.string.shieldSetupStatus_roles_atLeastOneFactor),
            type = StatusMessage.Type.ERROR
        )
    )
}

@Composable
fun ShieldSetupUnsafeCombinationStatusView(
    modifier: Modifier = Modifier,
    onInfoClick: (GlossaryItem) -> Unit
) {
    StatusMessageText(
        modifier = modifier.noIndicationClickable { onInfoClick(GlossaryItem.buildingshield) },
        message = StatusMessage(
            message = stringResource(id = R.string.shieldSetupStatus_unsafeCombination).formattedSpans(
                boldStyle = SpanStyle(
                    color = RadixTheme.colors.blue2,
                    fontWeight = RadixTheme.typography.body1StandaloneLink.fontWeight,
                    fontSize = RadixTheme.typography.body2Link.fontSize
                )
            ),
            type = StatusMessage.Type.WARNING
        )
    )
}
