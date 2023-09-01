@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.TransactionVersion
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel.State
import rdx.works.profile.data.model.apppreferences.Radix

@Composable
fun TransactionPreviewHeader(
    modifier: Modifier = Modifier,
    state: State,
    onBackClick: () -> Unit,
    onRawManifestClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val fraction = scrollBehavior.state.collapsedFraction
    LargeTopAppBar(
        modifier = modifier,
        title = {
            Box {
                Text(
                    modifier = Modifier.alpha(1 - fraction * 2),
                    text = stringResource(R.string.transactionReview_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Start,
                    maxLines = 2
                )

                val smallAlpha = if (fraction in 0f..0.6f) 0f else fraction + fraction * 0.6f
                Text(
                    modifier = Modifier.alpha(smallAlpha).align(Alignment.Center),
                    text = stringResource(R.string.transactionReview_title),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick
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
            if (state.isRawManifestToggleVisible) {
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
            }
        },
        windowInsets = WindowInsets.statusBars,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = RadixTheme.colors.defaultBackground,
            scrolledContainerColor = RadixTheme.colors.defaultBackground
        ),
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun TransactionPreviewHeaderPreview() {
    RadixWalletTheme {
        TransactionPreviewHeader(
            onBackClick = {},
            state = State(
                request = MessageFromDataChannel.IncomingRequest.TransactionRequest(
                    remoteConnectorId = "",
                    requestId = "",
                    transactionManifestData = TransactionManifestData(
                        instructions = "",
                        version = TransactionVersion.Default.value,
                        networkId = Radix.Gateway.default.network.id,
                        message = "Hello"
                    ),
                    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata.internal(Radix.Gateway.default.network.id)
                ),
                isLoading = false,
                previewType = PreviewType.None
            ),
            onRawManifestClick = {},
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        )
    }
}
