package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState

@Composable
fun AddLedgerBottomSheet(
    modifier: Modifier,
    hasP2pLinks: Boolean,
    onAddP2PLink: () -> Unit,
    onSendAddLedgerRequest: () -> Unit,
    addLedgerSheetState: AddLedgerSheetState,
    onConfirmLedgerName: (String) -> Unit,
    onSkipLedgerName: () -> Unit,
    waitingForLedgerResponse: Boolean
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var ledgerNameValue by remember {
                mutableStateOf("")
            }
            when (addLedgerSheetState) {
                AddLedgerSheetState.Connect -> {
                    if (!hasP2pLinks) {
                        Text(
                            text = stringResource(id = com.babylon.wallet.android.R.string.ledgerImport_noConnections),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        RadixSecondaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onAddP2PLink,
                            text = stringResource(id = com.babylon.wallet.android.R.string.ledgerImport_addNewConnection)
                        )
                    }
                    Text(
                        text = stringResource(id = com.babylon.wallet.android.R.string.addLedger_addDevice_body2),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    RadixSecondaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSendAddLedgerRequest,
                        text = stringResource(id = com.babylon.wallet.android.R.string.ledgerImport_addLedger),
                        enabled = hasP2pLinks
                    )
                }
                AddLedgerSheetState.InputLedgerName -> {
                    RadixTextField(
                        modifier = Modifier.fillMaxWidth(),
                        onValueChanged = { ledgerNameValue = it },
                        value = ledgerNameValue,
                        leftLabel = stringResource(id = com.babylon.wallet.android.R.string.ledgerImport_textFieldLabel),
                        hint = stringResource(id = com.babylon.wallet.android.R.string.ledgerImport_textFieldPlaceholder),
                        optionalHint = stringResource(id = com.babylon.wallet.android.R.string.ledgerImport_textFieldHint)
                    )
                    RadixPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(com.babylon.wallet.android.R.string.common_confirm),
                        onClick = {
                            onConfirmLedgerName(ledgerNameValue)
                            ledgerNameValue = ""
                        },
                        throttleClicks = true
                    )
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding(),
                        text = stringResource(com.babylon.wallet.android.R.string.common_skip),
                        onClick = onSkipLedgerName,
                        throttleClicks = true
                    )
                }
            }
        }
        if (waitingForLedgerResponse) {
            FullscreenCircularProgressContent()
        }
    }
}
