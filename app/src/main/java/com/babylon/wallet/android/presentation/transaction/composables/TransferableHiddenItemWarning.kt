package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.InfoLink

@Composable
fun TransferableHiddenItemWarning(
    modifier: Modifier = Modifier,
    isHidden: Boolean
) {
    if (isHidden) {
        Column(
            modifier = modifier
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            InfoLink(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.transactionReview_hiddenAsset),
                contentColor = RadixTheme.colors.orange1,
                iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error,
                textStyle = RadixTheme.typography.body1Header
            )
        }
    }
}
