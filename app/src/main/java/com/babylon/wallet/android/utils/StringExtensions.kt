@file:Suppress("MagicNumber")

package com.babylon.wallet.android.utils

import android.util.Patterns
import android.webkit.URLUtil
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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

fun String.formattedSpans(
    boldStyle: SpanStyle
): AnnotatedString {
    val asteriskRegex = "(?<!\\*\\*)\\*\\*(?!\\*\\*).*?(?<!\\*\\*)\\*\\*(?!\\*\\*)".toRegex()
    val annotatedWords = asteriskRegex.findAll(input = this).map { it.value }.toList()
    return buildAnnotatedString {
        var startIndex = 0
        val inputText = this@formattedSpans
        annotatedWords.forEach { word ->
            val indexOfThisWord = inputText.indexOf(word)
            append(inputText.substring(startIndex, indexOfThisWord))

            startIndex = indexOfThisWord + word.length
            val strippedFromAnnotations = word.removeSurrounding("**")
            withStyle(boldStyle) {
                append(strippedFromAnnotations)
            }
        }
        append(inputText.substring(startIndex, inputText.length))
    }
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
    return URLEncoder.encode(this, "UTF-8")
}

fun String.decodeUtf8(): String {
    return URLDecoder.decode(this, "UTF-8")
}

fun String.prependHttpsPrefixIfNotPresent(): String {
    return if (this.startsWith("https://")) {
        this
    } else {
        "https://$this"
    }
}
