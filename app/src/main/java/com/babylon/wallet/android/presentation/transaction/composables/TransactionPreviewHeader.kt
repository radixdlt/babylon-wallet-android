package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun TransactionPreviewHeader(
    onBackClick: () -> Unit,
    onRawManifestClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBackEnabled: Boolean
) {
    Row(modifier) {
        IconButton(
            onClick = onBackClick,
            enabled = onBackEnabled
        ) {
            Icon(
                painterResource(
                    id = R.drawable.ic_close
                ),
                tint = RadixTheme.colors.gray1,
                contentDescription = "close"
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            modifier = Modifier
                .background(
                    color = RadixTheme.colors.gray4,
                    shape = RadixTheme.shapes.roundedRectSmall
                ),
            onClick = onRawManifestClick
        ) {
            Icon(
                painterResource(
                    id = R.drawable.ic_manifest_expand
                ),
                tint = Color.Unspecified,
                contentDescription = "manifest expand"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionPreviewHeaderPreview() {
    TransactionPreviewHeader(
        {},
        {},
        onBackEnabled = true
    )
}
