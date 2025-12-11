package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.securityshields.EmergencyFallbackView
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State.Sheet.ConfirmTimedRecovery
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.WarningButton
import com.babylon.wallet.android.presentation.ui.none
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample

@Composable
fun ConfirmTimedRecoverySheet(
    modifier: Modifier = Modifier,
    state: ConfirmTimedRecovery,
    onDismiss: () -> Unit,
    onRestartSigning: () -> Unit,
    onConfirm: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        BottomDialogHeader(
            modifier = Modifier.fillMaxWidth(),
            onDismissRequest = onDismiss
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = stringResource(id = R.string.confirmAccountRecovery_title),
            style = RadixTheme.typography.title,
            textAlign = TextAlign.Center,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = stringResource(id = R.string.confirmAccountRecovery_subtitle),
            style = RadixTheme.typography.body1Header,
            textAlign = TextAlign.Center,
            color = RadixTheme.colors.text
        )

        state.time?.let { time ->
            EmergencyFallbackView(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .padding(top = RadixTheme.dimensions.paddingSemiLarge),
                delay = time,
                description = AnnotatedString(
                    text = stringResource(id = R.string.confirmAccountRecovery_description)
                ),
                timePeriodTitle = stringResource(id = R.string.confirmAccountRecovery_confirmInLabel),
                onInfoClick = onInfoClick
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        RadixBottomBar(
            text = stringResource(R.string.transactionRecovery_restart),
            onClick = onRestartSigning,
            insets = WindowInsets.none,
            additionalTopContent = {
                WarningButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(bottom = RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.confirmAccountRecovery_useButton),
                    onClick = onConfirm
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
@UsesSampleValues
private fun ConfirmTimedRecoverySheetSheetPreview() {
    RadixWalletPreviewTheme {
        ConfirmTimedRecoverySheet(
            state = ConfirmTimedRecovery(
                time = TimePeriod.sample()
            ),
            onDismiss = {},
            onRestartSigning = {},
            onConfirm = {},
            onInfoClick = {}
        )
    }
}

@Preview
@Composable
@UsesSampleValues
private fun ConfirmTimedRecoverySheetSheetDarkPreview() {
    RadixWalletPreviewTheme(
        enableDarkTheme = true
    ) {
        ConfirmTimedRecoverySheet(
            state = ConfirmTimedRecovery(
                time = TimePeriod.sample()
            ),
            onDismiss = {},
            onRestartSigning = {},
            onConfirm = {},
            onInfoClick = {}
        )
    }
}
