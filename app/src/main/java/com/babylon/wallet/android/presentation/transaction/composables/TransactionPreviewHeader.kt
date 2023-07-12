@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
    val collapsed = scrollBehavior.state.collapsedFraction >= 0.5f
    LargeTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.transactionReview_title),
                style = if (!collapsed) RadixTheme.typography.title else RadixTheme.typography.body1Header,
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
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = RadixTheme.colors.defaultBackground,
            scrolledContainerColor = RadixTheme.colors.defaultBackground
        ),
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
