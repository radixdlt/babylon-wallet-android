@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun TransactionPreviewHeader(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onRawManifestClick: () -> Unit,
    onBackEnabled: Boolean,
    scrollBehavior: TopAppBarScrollBehavior
) {
    LargeTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.transactionReview_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                enabled = onBackEnabled
            ) {
                Icon(
                    painterResource(
                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_close
                    ),
                    tint = RadixTheme.colors.gray1,
                    contentDescription = "close"
                )
            }
        },
        actions = {
            IconButton(
                modifier = Modifier
                    .padding(end = RadixTheme.dimensions.paddingDefault)
                    .background(
                        color = RadixTheme.colors.gray4,
                        shape = RadixTheme.shapes.roundedRectSmall
                    ),
                onClick = onRawManifestClick
            ) {
                Icon(
                    painterResource(
                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_manifest_expand
                    ),
                    tint = Color.Unspecified,
                    contentDescription = "manifest expand"
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Preview(showBackground = true)
@Composable
fun TransactionPreviewHeaderPreview() {
    RadixWalletTheme {
        TransactionPreviewHeader(
            onBackClick = {},
            onRawManifestClick = {},
            onBackEnabled = true,
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        )
    }
}
