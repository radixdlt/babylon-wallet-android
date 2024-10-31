package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.utils.formattedSpans
import java.util.Locale

@Composable
fun TransactionPreAuthorizationInfo(
    modifier: Modifier = Modifier,
    dAppName: String?,
    preAuthorization: State.PreAuthorization,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Info(
            dAppName = dAppName,
            onInfoClick = { onInfoClick(GlossaryItem.preauthorizations) }
        )

        preAuthorization.expiration?.let { expiration ->
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            Text(
                modifier = Modifier.padding(
                    horizontal = RadixTheme.dimensions.paddingDefault
                ),
                text = if (expiration.remainingSeconds > 0) {
                    stringResource(
                        id = if (expiration.isExpiringAtTime) {
                            R.string.preAuthorizationReview_expiration_atTime
                        } else {
                            R.string.preAuthorizationReview_expiration_afterDelay
                        },
                        formatTime(seconds = expiration.remainingSeconds)
                    )
                } else {
                    stringResource(id = R.string.preAuthorizationReview_expiration_expired)
                }.formattedSpans(boldStyle = RadixTheme.typography.body2HighImportance.toSpanStyle()),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.pink1
            )
        }
    }
}

@Composable
private fun Info(
    dAppName: String?,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.gray5,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingSemiLarge
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(
                    id = R.string.preAuthorizationReview_fees_title,
                    dAppName.orEmpty()
                        .ifEmpty { stringResource(id = R.string.dAppRequest_metadata_unknownName) }
                ),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )

            Text(
                text = stringResource(id = R.string.preAuthorizationReview_fees_subtitle),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXXLarge))

        Box(
            modifier = Modifier
                .padding(
                    start = RadixTheme.dimensions.paddingXXLarge,
                    end = RadixTheme.dimensions.paddingXSmall
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onInfoClick
                )
        ) {
            InfoButton(
                text = stringResource(id = R.string.empty),
                color = RadixTheme.colors.gray2,
                onClick = onInfoClick
            )
        }
    }
}

/**
 * Given an amount of seconds, returns a formatted String using the corresponding unit (days/hours/minutes/seconds).
 * A few examples on how should it look for each of them:
 * - `8 days` / `1 day`
 * - `23:21 hours` / `1:24 hour`
 * - `56:02 minutes` / `1:23 minute`
 * - `34 seconds` / `1 second`
 */
@Composable
fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> if (days == 1L) {
            stringResource(id = R.string.preAuthorizationReview_timeFormat_day)
        } else {
            stringResource(id = R.string.preAuthorizationReview_timeFormat_days, days)
        }
        hours > 0 -> {
            val remainingMinutes = minutes % 60
            val formatted = String.format(Locale.getDefault(), "%d:%02d", hours, remainingMinutes)
            if (hours == 1L) {
                stringResource(id = R.string.preAuthorizationReview_timeFormat_hour, formatted)
            } else {
                stringResource(id = R.string.preAuthorizationReview_timeFormat_hours, formatted)
            }
        }
        minutes > 0 -> {
            val remainingSeconds = seconds % 60
            val formatted = String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
            if (minutes == 1L) {
                stringResource(id = R.string.preAuthorizationReview_timeFormat_minute, formatted)
            } else {
                stringResource(id = R.string.preAuthorizationReview_timeFormat_minutes, formatted)
            }
        }
        else -> if (seconds == 1L) {
            stringResource(id = R.string.preAuthorizationReview_timeFormat_second)
        } else {
            stringResource(id = R.string.preAuthorizationReview_timeFormat_seconds, seconds)
        }
    }
}

@Composable
@Preview
private fun TransactionPreAuthorizationInfoPreview() {
    RadixWalletPreviewTheme {
        TransactionPreAuthorizationInfo(
            dAppName = "Megaswap",
            preAuthorization = State.PreAuthorization(
                expiration = State.PreAuthorization.Expiration(
                    isExpiringAtTime = true,
                    remainingSeconds = 12345
                )
            ),
            onInfoClick = {}
        )
    }
}
