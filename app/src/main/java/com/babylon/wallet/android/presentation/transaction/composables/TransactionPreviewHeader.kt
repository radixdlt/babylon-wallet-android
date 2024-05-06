@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.TwoRowsTopAppBar
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.radixdlt.sargon.Gateway
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.TransactionVersion
import rdx.works.core.sargon.default

@Composable
fun TransactionPreviewHeader(
    modifier: Modifier = Modifier,
    state: State,
    onBackClick: () -> Unit,
    onRawManifestClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    TwoRowsTopAppBar(
        modifier = modifier,
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = RadixTheme.dimensions.paddingXXLarge)
                    .padding(end = RadixTheme.dimensions.paddingXLarge)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CompositionLocalProvider(
                            value = LocalDensity provides Density(
                                density = LocalDensity.current.density,
                                fontScale = 1f
                            )
                        ) {
                            Text(
                                modifier = Modifier.weight(1.5f),
                                text = stringResource(R.string.transactionReview_title),
                                color = RadixTheme.colors.gray1,
                                textAlign = TextAlign.Start,
                                maxLines = 2,
                            )
                        }
                        if (state.proposingDApp?.iconUrl != null) {
                            Thumbnail.DApp(
                                modifier = Modifier
                                    .size(64.dp),
                                dapp = state.proposingDApp,
                                shape = RadixTheme.shapes.roundedRectSmall
                            )
                        }
                    }
                    if (state.request?.isInternal != true) {
                        val dAppName = state.proposingDApp?.name.orEmpty().ifEmpty {
                            stringResource(id = R.string.dAppRequest_metadata_unknownName)
                        }
                        Text(
                            text = stringResource(id = R.string.transactionReview_proposingDappSubtitle, dAppName),
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.gray1,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        titleTextStyle = RadixTheme.typography.title,
        smallTitle = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.transactionReview_title),
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        },
        smallTitleTextStyle = RadixTheme.typography.secondaryHeader,
        navigationIcon = {
            IconButton(
                modifier = Modifier
                    .padding(start = RadixTheme.dimensions.paddingDefault),
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
                val icon = if (state.isRawManifestVisible) {
                    com.babylon.wallet.android.designsystem.R.drawable.ic_manifest_collapse
                } else {
                    com.babylon.wallet.android.designsystem.R.drawable.ic_manifest_expand
                }
                IconButton(
                    modifier = Modifier
                        .padding(end = RadixTheme.dimensions.paddingXLarge)
                        .background(
                            color = RadixTheme.colors.gray4,
                            shape = RadixTheme.shapes.roundedRectSmall
                        )
                        .size(width = 50.dp, height = 40.dp),
                    onClick = onRawManifestClick
                ) {
                    Icon(
                        painter = painterResource(
                            id = icon
                        ),
                        tint = Color.Unspecified,
                        contentDescription = "manifest expand"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        titleBottomPadding = RadixTheme.dimensions.paddingSemiLarge,
        windowInsets = TopAppBarDefaults.windowInsets,
        maxHeight = 200.dp,
        pinnedHeight = 82.dp,
        scrollBehavior = scrollBehavior
    )
}

@Preview(showBackground = true)
@Composable
fun TransactionPreviewHeaderPreview() {
    RadixWalletTheme {
        TransactionPreviewHeader(
            onBackClick = {},
            state = State(
                request = IncomingMessage.IncomingRequest.TransactionRequest(
                    remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId(""),
                    interactionId = "",
                    transactionManifestData = TransactionManifestData(
                        instructions = "",
                        networkId = Gateway.default.network.id,
                        message = TransactionManifestData.TransactionMessage.Public("Hello"),
                        version = TransactionVersion.Default.value
                    ),
                    requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata.internal(Gateway.default.network.id)
                ),
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.None,
            ),
            onRawManifestClick = {},
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        )
    }
}
