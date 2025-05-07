package com.babylon.wallet.android.presentation.dialogs.preauthorization

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.Pink1
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.transaction.FailureDialogContent
import com.babylon.wallet.android.presentation.dialogs.transaction.SuccessContent
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.dAppDisplayName
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.TimeFormatter
import com.babylon.wallet.android.utils.copyToClipboard
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlin.time.Duration.Companion.seconds

@Composable
fun PreAuthorizationStatusDialog(
    modifier: Modifier = Modifier,
    viewModel: PreAuthorizationStatusViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val dismissHandler = {
        viewModel.onDismiss()
    }

    BackHandler {
        dismissHandler()
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                PreAuthorizationStatusViewModel.Event.Dismiss -> onDismiss()
                is PreAuthorizationStatusViewModel.Event.PerformCopy -> context.copyToClipboard(
                    label = "Pre-Authorization ID",
                    value = event.valueToCopy
                )
            }
        }
    }

    PreAuthorizationStatusContent(
        modifier = modifier,
        state = state,
        onPreAuthorizationIdClick = viewModel::onCopyPreAuthorizationIdClick,
        onDismiss = dismissHandler
    )
}

@Composable
private fun PreAuthorizationStatusContent(
    modifier: Modifier = Modifier,
    state: PreAuthorizationStatusViewModel.State,
    onPreAuthorizationIdClick: () -> Unit,
    onDismiss: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier,
        onDismiss = onDismiss,
        dragToDismissEnabled = true,
        showDragHandle = true,
        isDismissible = false,
        content = {
            when (state.status) {
                is PreAuthorizationStatusViewModel.State.Status.Sent -> SentContent(
                    status = state.status,
                    onPreAuthorizationIdClick = onPreAuthorizationIdClick
                )
                is PreAuthorizationStatusViewModel.State.Status.Success -> SuccessContent(
                    modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingXXLarge),
                    transactionId = state.status.transactionId,
                    isMobileConnect = state.status.isMobileConnect,
                    title = stringResource(id = R.string.transactionStatus_success_title),
                    subtitle = stringResource(R.string.transactionStatus_success_text)
                )
                is PreAuthorizationStatusViewModel.State.Status.Expired -> FailureDialogContent(
                    modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingXXLarge),
                    title = stringResource(id = R.string.preAuthorizationReview_expiredStatus_title),
                    subtitle = stringResource(id = R.string.preAuthorizationReview_expiredStatus_subtitle),
                    transactionId = null,
                    isMobileConnect = state.status.isMobileConnect
                )
            }
        }
    )
}

@Composable
private fun SentContent(
    modifier: Modifier = Modifier,
    status: PreAuthorizationStatusViewModel.State.Status.Sent,
    onPreAuthorizationIdClick: () -> Unit
) {
    val dAppName = status.dAppName.dAppDisplayName()

    Column(
        modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.background)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(color = RadixTheme.colors.background)
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            Image(
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline_large
                ),
                contentDescription = null
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

            Text(
                text = stringResource(R.string.preAuthorizationReview_unknownStatus_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

            Text(
                text = stringResource(id = R.string.preAuthorizationReview_unknownStatus_subtitle, dAppName),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            PreAuthorizationId(
                id = status.preAuthorizationId,
                onClick = onPreAuthorizationIdClick
            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        HorizontalDivider(color = RadixTheme.colors.divider)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = RadixTheme.colors.backgroundSecondary)
                .padding(
                    start = RadixTheme.dimensions.paddingLarge,
                    end = RadixTheme.dimensions.paddingLarge,
                    top = RadixTheme.dimensions.paddingMedium,
                    bottom = RadixTheme.dimensions.paddingLarge
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpirationContent(
                dAppName = dAppName,
                expiration = status.expiration
            )
        }
    }
}

@Composable
private fun ExpirationContent(
    dAppName: String,
    expiration: PreAuthorizationStatusViewModel.State.Status.Sent.Expiration
) {
    val context = LocalContext.current

    val time = remember(expiration) {
        TimeFormatter.format(context, expiration.duration, expiration.truncateSeconds)
    }

    Text(
        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
        text = if (expiration.isCheckingOneLastTime) {
            stringResource(
                id = R.string.preAuthorizationReview_unknownStatus_lastCheck
            ).formattedSpans(boldStyle = RadixTheme.typography.body2HighImportance.toSpanStyle())
        } else {
            stringResource(
                id = R.string.preAuthorizationReview_unknownStatus_expiration,
                dAppName,
                time
            ).formattedSpans(boldStyle = RadixTheme.typography.body2HighImportance.toSpanStyle())
        },
        style = RadixTheme.typography.body2Regular,
        color = Pink1,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PreAuthorizationId(
    modifier: Modifier = Modifier,
    id: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.preAuthorizationReview_unknownStatus_identifier),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXXSmall))

        Row(
            modifier = Modifier.throttleClickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = id,
                color = RadixTheme.colors.textButton,
                maxLines = 1,
                style = RadixTheme.typography.body1HighImportance
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXXSmall))

            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = null,
                tint = RadixTheme.colors.iconSecondary
            )
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun PreAuthorizationStatusPreviewLight(
    @PreviewParameter(PreAuthorizationStatusPreviewProvider::class) state: PreAuthorizationStatusViewModel.State
) {
    RadixWalletPreviewTheme {
        PreAuthorizationStatusContent(
            state = state,
            onPreAuthorizationIdClick = {},
            onDismiss = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun PreAuthorizationStatusPreviewDark(
    @PreviewParameter(PreAuthorizationStatusPreviewProvider::class) state: PreAuthorizationStatusViewModel.State
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        PreAuthorizationStatusContent(
            state = state,
            onPreAuthorizationIdClick = {},
            onDismiss = {}
        )
    }
}


@UsesSampleValues
class PreAuthorizationStatusPreviewProvider : PreviewParameterProvider<PreAuthorizationStatusViewModel.State> {

    override val values: Sequence<PreAuthorizationStatusViewModel.State>
        get() = sequenceOf(
            PreAuthorizationStatusViewModel.State(
                status = PreAuthorizationStatusViewModel.State.Status.Sent(
                    preAuthorizationId = "PAid...0runll",
                    dAppName = "Collabo.Fi",
                    expiration = PreAuthorizationStatusViewModel.State.Status.Sent.Expiration(
                        duration = 0.seconds
                    )
                )
            ),
            PreAuthorizationStatusViewModel.State(
                status = PreAuthorizationStatusViewModel.State.Status.Expired(
                    isMobileConnect = true
                )
            ),
            PreAuthorizationStatusViewModel.State(
                status = PreAuthorizationStatusViewModel.State.Status.Success(
                    transactionId = TransactionIntentHash.sample(),
                    isMobileConnect = true
                )
            )
        )
}
