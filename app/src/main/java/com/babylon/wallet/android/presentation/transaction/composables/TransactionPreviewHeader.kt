package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.TransactionVersion
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.ui.composables.ReceiptEdge
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import rdx.works.profile.data.model.apppreferences.Radix

@Composable
fun TransactionPreviewHeader(
    modifier: Modifier = Modifier,
    state: State,
    onBackClick: () -> Unit,
    onRawManifestClick: () -> Unit,
    scrollState: ScrollState
) {
    val context = LocalContext.current
    val motionSceneContent = remember {
        context.resources
            .openRawResource(R.raw.transaction_review_top_bar_scene)
            .readBytes()
            .decodeToString()
    }

    // size of the preview header
    var size by remember { mutableStateOf(IntSize.Zero) }

    val animationRangePx = with(LocalDensity.current) { 200.dp.toPx() }
    val progress by remember {
        derivedStateOf {
            // if max value of the scroll is less than the height of the preview header
            // then don't use the animation
            if (scrollState.maxValue <= size.height) {
                0f
            } else {
                (scrollState.value / animationRangePx).coerceIn(0f, 1f)
            }
        }
    }

    MotionLayout(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .onGloballyPositioned { layoutCoordinates ->
                size = layoutCoordinates.size
            },
        motionScene = MotionScene(
            content = motionSceneContent,
        ),
        progress = progress
    ) {
        CompositionLocalProvider(
            value = LocalDensity provides Density(
                density = LocalDensity.current.density,
                fontScale = 1f
            )
        ) {
            Text(
                modifier = Modifier.layoutId("title"),
                text = stringResource(R.string.transactionReview_title),
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Start,
                maxLines = 2,
                style = RadixTheme.typography.title.copy(
                    fontSize = lerp(
                        start = RadixTheme.typography.title.fontSize,
                        stop = 18.sp,
                        fraction = progress
                    )
                )
            )
            if (state.request?.isInternal != true) {
                val dAppName = state.proposingDApp?.name.orEmpty().ifEmpty {
                    stringResource(
                        id = R.string.dAppRequest_metadata_unknownName
                    )
                }
                Text(
                    modifier = Modifier.layoutId("subtitle"),
                    text = stringResource(id = R.string.transactionReview_proposingDappSubtitle, dAppName),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        state.proposingDApp?.iconUrl?.let {
            Thumbnail.DApp(
                modifier = Modifier
                    .layoutId("dAppIcon")
                    .size(64.dp),
                dapp = state.proposingDApp,
                shape = RadixTheme.shapes.roundedRectSmall
            )
        }
        IconButton(
            modifier = Modifier.layoutId("closeButton"),
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
        if (state.isRawManifestToggleVisible) {
            IconButton(
                modifier = Modifier
                    .layoutId("rawManifestButton")
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

        ReceiptEdge(
            modifier = Modifier.layoutId("receiptEdge").fillMaxWidth(),
            color = RadixTheme.colors.gray5,
            topEdge = true
        )
    }
}

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
                isNetworkFeeLoading = false,
                previewType = PreviewType.None
            ),
            onRawManifestClick = {},
            scrollState = ScrollState(0)
        )
    }
}
