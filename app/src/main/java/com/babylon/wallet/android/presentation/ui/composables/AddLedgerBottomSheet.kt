package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState

@Composable
fun AddLedgerBottomSheet(
    modifier: Modifier,
    onSendAddLedgerRequest: () -> Unit,
    addLedgerSheetState: AddLedgerSheetState,
    onConfirmLedgerName: (String) -> Unit,
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
                    Text(
                        text = stringResource(id = com.babylon.wallet.android.R.string.addLedger_addDevice_body1),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Text(
                        text = stringResource(id = com.babylon.wallet.android.R.string.addLedger_addDevice_body2),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    RadixPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSendAddLedgerRequest,
                        text = stringResource(id = com.babylon.wallet.android.R.string.addLedger_addDevice_continue)
                    )
                }
                AddLedgerSheetState.InputLedgerName -> {
                    Text(
                        text = stringResource(id = com.babylon.wallet.android.R.string.addLedger_nameLedger_title),
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.gray1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = com.babylon.wallet.android.R.string.addLedger_nameLedger_body),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    RadixTextField(
                        modifier = Modifier.fillMaxWidth(),
                        onValueChanged = { ledgerNameValue = it },
                        value = ledgerNameValue,
                        hint = stringResource(id = com.babylon.wallet.android.R.string.addLedger_nameLedger_namePlaceholder),
                    )
                    RadixPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(com.babylon.wallet.android.R.string.common_confirm),
                        onClick = {
                            onConfirmLedgerName(ledgerNameValue)
                            ledgerNameValue = ""
                        }
                    )
                }
            }
        }
        if (waitingForLedgerResponse) {
            FullscreenCircularProgressContent()
        }
    }
}
