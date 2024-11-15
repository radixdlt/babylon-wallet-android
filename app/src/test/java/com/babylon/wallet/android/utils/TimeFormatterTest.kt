package com.babylon.wallet.android.utils

import android.content.Context
import com.babylon.wallet.android.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class TimeFormatterTest(val vector: TestVector) {

    val context = mockk<Context>().apply {
        every { getString(R.string.preAuthorizationReview_timeFormat_day) } returns "1 day"
        val daysSlot = slot<Long>()
        every { getString(R.string.preAuthorizationReview_timeFormat_days, capture(daysSlot)) } answers { "${daysSlot.captured} days" }

        val hoursFormattedSlot = slot<String>()
        every {
            getString(R.string.preAuthorizationReview_timeFormat_hour, capture(hoursFormattedSlot))
        } answers { "${hoursFormattedSlot.captured} hour" }
        every {
            getString(R.string.preAuthorizationReview_timeFormat_hours, capture(hoursFormattedSlot))
        } answers { "${hoursFormattedSlot.captured} hours" }

        val minutesFormattedSlot = slot<String>()
        every {
            getString(R.string.preAuthorizationReview_timeFormat_minute, capture(minutesFormattedSlot))
        } answers { "${minutesFormattedSlot.captured} minute" }
        every {
            getString(R.string.preAuthorizationReview_timeFormat_minutes, capture(minutesFormattedSlot))
        } answers { "${minutesFormattedSlot.captured} minutes" }

        every {
            getString(R.string.preAuthorizationReview_timeFormat_second)
        } returns "1 second"

        val secondsSlot = slot<Long>()
        every {
            getString(R.string.preAuthorizationReview_timeFormat_seconds, capture(secondsSlot))
        } answers { "${secondsSlot.captured} seconds" }
    }

    @Test
    fun test() {
        assertEquals(
            vector.output,
            TimeFormatter.format(context, vector.duration, false),
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            TestVector(1.seconds to "1 second"),
            TestVector(34.seconds to "34 seconds"),
            TestVector((1.minutes + 23.seconds) to "1:23 minute"),
            TestVector((56.minutes + 2.seconds) to "56:02 minutes"),
            TestVector((1.hours + 24.minutes) to "1:24 hour"),
            TestVector((23.hours + 21.minutes) to "23:21 hours"),
            TestVector(1.days to "1 day"),
            TestVector(8.days to "8 days")
        )

        data class TestVector(
            val duration: Duration,
            val output: String
        ) {
            constructor(pair: Pair<Duration, String>): this(duration = pair.first, output = pair.second)
        }
    }
}