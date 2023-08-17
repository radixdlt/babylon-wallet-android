package com.babylon.wallet.android.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

const val LAST_USED_DATE_FORMAT = "d MMM yyyy"
const val LAST_USED_DATE_FORMAT_THIS_YEAR = "d MMM"

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

@Suppress("SwallowedException")
fun String.fromISO8601String(): LocalDateTime? {
    return try {
        LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(this))
    } catch (exception: Exception) {
        // TODO this may be removed when we have enough confidence that no users would fall into this case
        null // catch those existing events with the LocalDate date from the existing accounts.
    }
}

fun Instant.ledgerLastUsedDateFormat(): String {
    val zoneId = ZoneId.systemDefault()
    val currentYear = Instant.now().atZone(zoneId).year
    val instantYear = atZone(zoneId).year
    val format = if (currentYear == instantYear) {
        LAST_USED_DATE_FORMAT_THIS_YEAR
    } else {
        LAST_USED_DATE_FORMAT
    }
    val formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault())
    return formatter.format(this)
}

fun Instant.toDateString(): String {
    val formatter = DateTimeFormatter.ofPattern(LAST_USED_DATE_FORMAT).withZone(ZoneId.systemDefault())
    return formatter.format(this)
}
