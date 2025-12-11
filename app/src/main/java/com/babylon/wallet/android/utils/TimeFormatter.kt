package com.babylon.wallet.android.utils

import android.content.Context
import com.babylon.wallet.android.R
import kotlin.time.Duration

object TimeFormatter {

    /**
     * Given an amount of seconds, returns a formatted String using the corresponding unit (days/hours/minutes/seconds).
     * A few examples on how should it look for each of them:
     * - `8 days` / `1 day`
     * - `23:21 hours` / `1:24 hour`
     * - `56:02 minutes` / `1:23 minute`
     * - `34 seconds` / `1 second`
     */
    @Suppress("MagicNumber")
    fun format(context: Context, duration: Duration, truncateSeconds: Boolean): String {
        val seconds = duration.inWholeSeconds
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return if (days > 0) {
            if (days == 1L) {
                context.getString(R.string.preAuthorizationReview_timeFormat_day)
            } else {
                context.getString(R.string.preAuthorizationReview_timeFormat_days, days)
            }
        } else if (hours > 0) {
            val remainingMinutes = minutes % 60
            val hoursFormatted = "%d:%02d".format(hours, remainingMinutes)
            if (hours == 1L) {
                context.getString(R.string.preAuthorizationReview_timeFormat_hour, hoursFormatted)
            } else {
                context.getString(R.string.preAuthorizationReview_timeFormat_hours, hoursFormatted)
            }
        } else if (minutes > 0) {
            val remainingSeconds = seconds % 60
            val minutesFormatted = if (truncateSeconds) "%d".format(minutes) else "%d:%02d".format(minutes, remainingSeconds)
            if (minutes == 1L) {
                context.getString(R.string.preAuthorizationReview_timeFormat_minute, minutesFormatted)
            } else {
                context.getString(R.string.preAuthorizationReview_timeFormat_minutes, minutesFormatted)
            }
        } else {
            if (seconds == 1L) {
                context.getString(R.string.preAuthorizationReview_timeFormat_second)
            } else {
                context.getString(R.string.preAuthorizationReview_timeFormat_seconds, seconds)
            }
        }
    }

    @Suppress("MagicNumber")
    fun formatShortTruncatedToHours(duration: Duration): String {
        val seconds = duration.inWholeSeconds
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return if (days > 0) {
            "${days}d"
        } else if (hours > 0) {
            "${hours}h"
        } else {
            "<1h"
        }
    }
}
