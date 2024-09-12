package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.WarningText

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
            WarningText(
                modifier = Modifier.fillMaxWidth(),
                text = AnnotatedString(text),
                textStyle = RadixTheme.typography.body1Header
            )
        }
    }
}
