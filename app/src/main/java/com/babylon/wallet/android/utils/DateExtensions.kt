package com.babylon.wallet.android.utils

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

const val LAST_USED_PERSONA_DATE_FORMAT = "d MMM yyyy"

fun LocalDateTime.toISO8601String(): String {
    return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(
        this.atZone(
            ZoneId.systemDefault()
        ).withZoneSameInstant(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.SECONDS)
    )
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
