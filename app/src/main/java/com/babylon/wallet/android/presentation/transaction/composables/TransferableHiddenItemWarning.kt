package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.InfoLink

@Composable
fun TransferableHiddenItemWarning(
    modifier: Modifier = Modifier,
    isHidden: Boolean,
    text: String
) {
    if (isHidden) {
        Column(
            modifier = modifier
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            InfoLink(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                contentColor = RadixTheme.colors.orange1,
                iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error,
                textStyle = RadixTheme.typography.body1Header
            )
        }
    }
}
