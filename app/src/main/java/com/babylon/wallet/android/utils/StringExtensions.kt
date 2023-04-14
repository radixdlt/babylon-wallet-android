@file:Suppress("MagicNumber")

package com.babylon.wallet.android.utils

import android.util.Patterns
import android.webkit.URLUtil
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import okio.ByteString.Companion.decodeBase64
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.DecimalFormat

fun String.truncatedHash(): String {
    if (length <= 9) {
        return this
    }
    val first = substring(IntRange(0, 3))
    val last = substring(IntRange(length - 6, length - 1))
    return "$first...$last"
}

fun String.setSpanForPlaceholder(placeholder: String, spanStyle: SpanStyle): AnnotatedString {
    decodeBase64()
    val index = indexOf(placeholder)
    if (index == -1) return AnnotatedString(this)
    val spans = listOf(
        AnnotatedString.Range(
            spanStyle,
            index,
            index + placeholder.length
        ),
    )
    return AnnotatedString(this, spanStyles = spans)
}

fun AnnotatedString.setSpanForPlaceholder(placeholder: String, spanStyle: SpanStyle): AnnotatedString {
    val index = indexOf(placeholder)
    if (index == -1) return this
    val spans = listOf(
        AnnotatedString.Range(
            spanStyle,
            index,
            index + placeholder.length
        ),
    )
    return AnnotatedString(this.text, spanStyles = this.spanStyles + spans)
}

fun String.formatDecimalSeparator(): String {
    if (isEmpty()) return this
    val formatter = DecimalFormat("#,###.##")
    val amount = replace(",", "")
    return try {
        formatter.format(amount.toDouble())
    } catch (e: NumberFormatException) {
        ""
    }
}

fun String.isValidUrl(): Boolean {
    return Patterns.WEB_URL.matcher(this).matches()
}

fun String.isValidHttpsUrl(): Boolean {
    return Patterns.WEB_URL.matcher(this).matches() && URLUtil.isHttpsUrl(this)
}

fun String.isValidEmail(): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.encodeUtf8(): String {
    val encoded = URLEncoder.encode(this, "UTF-8")
    Timber.d("Encode: input $this, output $encoded")
    return encoded
}

fun String.decodeUtf8(): String {
    val decoded = URLDecoder.decode(this, "UTF-8")
    return decoded
}

fun String.prependHttpsPrefixIfNotPresent(): String {
    return if (this.startsWith("https://")) {
        this
    } else {
        "https://$this"
    }
}
