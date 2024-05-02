package com.babylon.wallet.android.utils

import com.radixdlt.sargon.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

const val LAST_USED_DATE_FORMAT_SHORT_MONTH = "d MMM yyyy"
const val LAST_USED_DATE_FORMAT_THIS_YEAR_SHORT_MONTH = "d MMM"
const val LAST_USED_DATE_FORMAT = "d MMMM yyyy"
const val LAST_USED_DATE_FORMAT_THIS_YEAR = "d MMMM"
const val TIMESTAMP_HOURS_MINUTES = "HH:mm"
const val TIMESTAMP_ONLY_MONTH = "MMM"
const val TIMESTAMP_MONTH_AND_YEAR = "MMM YY"

fun LocalDateTime.toISO8601String(): String {
    return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(
        this.atZone(
            ZoneId.systemDefault()
        ).withZoneSameInstant(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.SECONDS)
    )
}

fun LocalDateTime.toEpochMillis(): Long {
    return ZonedDateTime.of(this, ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Instant.dayMonthDateShort(): String {
    val zoneId = ZoneId.systemDefault()
    val currentYear = Instant.now().atZone(zoneId).year
    val instantYear = atZone(zoneId).year
    val format = if (currentYear == instantYear) {
        LAST_USED_DATE_FORMAT_THIS_YEAR_SHORT_MONTH
    } else {
        LAST_USED_DATE_FORMAT_SHORT_MONTH
    }
    val formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault())
    return formatter.format(this)
}

fun Instant.timestampHoursMinutes(): String {
    val formatter = DateTimeFormatter.ofPattern(TIMESTAMP_HOURS_MINUTES).withZone(ZoneId.systemDefault())
    return formatter.format(this)
}

fun Instant.toDateString(): String {
    val formatter = DateTimeFormatter.ofPattern(LAST_USED_DATE_FORMAT_SHORT_MONTH).withZone(ZoneId.systemDefault())
    return formatter.format(this)
}

fun Timestamp.toDateString(): String {
    val formatter = DateTimeFormatter.ofPattern(LAST_USED_DATE_FORMAT_SHORT_MONTH).withZone(ZoneId.systemDefault())
    return formatter.format(this)
}

fun ZonedDateTime.toMonthString(): String {
    val currentYear = Instant.now().atZone(zone).year
    val format = if (currentYear == year) {
        TIMESTAMP_ONLY_MONTH
    } else {
        TIMESTAMP_MONTH_AND_YEAR
    }
    val formatter = DateTimeFormatter.ofPattern(format)
    return formatter.format(this)
}
