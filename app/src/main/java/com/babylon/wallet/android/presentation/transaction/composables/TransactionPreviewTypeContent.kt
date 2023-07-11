package com.babylon.wallet.android.presentation.transaction.composables

import android.content.pm.ModuleInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity
import kotlinx.collections.immutable.toPersistentList

@Composable
fun TransactionPreviewTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionApprovalViewModel2.State,
    preview: PreviewType.Transaction,
    onApproveTransaction: () -> Unit
) {
    var showNotSecuredDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.background(RadixTheme.colors.gray5)
        ) {
            state.message?.let {
                TransactionMessageContent(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    transactionMessage = it
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }

            WithdrawAccountContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                from = preview.from
            )

            StrokeLine(height = 40.dp)

            DepositAccountContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                to = preview.to,
                shouldPromptForGuarantees = false,
                promptForGuarantees = {

                }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }

        PresentingProofsContent(badges = preview.badges.toPersistentList())

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        NetworkFeeContent(fees = state.fees)

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        val context = LocalContext.current
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.transactionReview_approveButtonTitle),
            onClick = {
                if (state.isDeviceSecure) {
                    context.findFragmentActivity()?.let { activity ->
                        activity.biometricAuthenticate(true) { authenticatedSuccessfully ->
                            if (authenticatedSuccessfully) {
                                onApproveTransaction()
                            }
                        }
                    }
                } else {
                    showNotSecuredDialog = true
                }
            },
            enabled = !state.isLoading && !state.isSigning
            /*&& state.canApprove && state.bottomSheetViewMode != BottomSheetMode.FeePayerSelection*/,
            icon = {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_lock),
                    contentDescription = ""
                )
            }
        )

    }

    if (showNotSecuredDialog) {
        NotSecureAlertDialog(
            finish = { accepted ->
                showNotSecuredDialog = false
                if (accepted) {
                    onApproveTransaction()
                }
            })
    }
}
