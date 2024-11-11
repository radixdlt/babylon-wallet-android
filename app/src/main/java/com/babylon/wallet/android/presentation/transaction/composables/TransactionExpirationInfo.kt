package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.utils.TimeFormatter
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.domain.DApp
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun TransactionExpirationInfo(
    modifier: Modifier = Modifier,
    expiration: TransactionReviewViewModel.State.Expiration,
    proposingDApp: TransactionReviewViewModel.State.ProposingDApp,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PreAuthorizationFeesHint(
            proposingDApp = proposingDApp,
            onClick = { onInfoClick(GlossaryItem.preauthorizations) }
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

        val context = LocalContext.current
        val time = remember(expiration) {
            TimeFormatter.format(context, expiration.duration, expiration.truncateSeconds)
        }

        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = if (expiration.isExpired) {
                AnnotatedString(stringResource(id = R.string.preAuthorizationReview_expiration_expired))
            } else if (expiration.startsAfterSign) {
                stringResource(
                    id = R.string.preAuthorizationReview_expiration_afterDelay,
                    time
                ).formattedSpans(boldStyle = RadixTheme.typography.body2HighImportance.toSpanStyle())
            } else {
                stringResource(
                    id = R.string.preAuthorizationReview_expiration_atTime,
                    time
                ).formattedSpans(boldStyle = RadixTheme.typography.body2HighImportance.toSpanStyle())
            },
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.pink1,
        )
    }
}

@Composable
fun PreAuthorizationFeesHint(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    proposingDApp: TransactionReviewViewModel.State.ProposingDApp
) {
    Row(
        modifier = modifier.background(
            color = RadixTheme.colors.gray5,
            shape = RadixTheme.shapes.roundedRectMedium
        ).clickable {
            onClick()
        }.padding(
            vertical = RadixTheme.dimensions.paddingDefault,
            horizontal = RadixTheme.dimensions.paddingSemiLarge
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.preAuthorizationReview_fees_title, proposingDApp.name.orEmpty()),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )

            Text(
                text = stringResource(R.string.preAuthorizationReview_fees_subtitle),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }

        Icon(
            painter = painterResource(com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline),
            tint = RadixTheme.colors.gray2,
            contentDescription = "info"
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun TransactionPreAuthorizationInfoAfterDelayPreview() {
    RadixWalletPreviewTheme {
        TransactionExpirationInfo(
            expiration = TransactionReviewViewModel.State.Expiration(
                duration = 5.hours + 23.minutes,
                startsAfterSign = true
            ),
            proposingDApp = TransactionReviewViewModel.State.ProposingDApp.Some(DApp.sampleMainnet()),
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun TransactionPreAuthorizationInfoTimePreview() {
    RadixWalletPreviewTheme {
        TransactionExpirationInfo(
            expiration = TransactionReviewViewModel.State.Expiration(
                duration = 5.hours + 23.minutes,
                startsAfterSign = false
            ),
            proposingDApp = TransactionReviewViewModel.State.ProposingDApp.Some(DApp.sampleMainnet()),
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun TransactionPreAuthorizationInfoExpiredPreview() {
    RadixWalletPreviewTheme {
        TransactionExpirationInfo(
            expiration = TransactionReviewViewModel.State.Expiration(
                duration = 0.seconds,
                startsAfterSign = false
            ),
            proposingDApp = TransactionReviewViewModel.State.ProposingDApp.Some(DApp.sampleMainnet()),
            onInfoClick = {}
        )
    }
}
