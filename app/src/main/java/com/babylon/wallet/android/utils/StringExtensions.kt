@file:Suppress("MagicNumber")

package com.babylon.wallet.android.utils

import android.util.Patterns
import android.webkit.URLUtil
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.radixdlt.bip39.wordlists.WORDLIST_ENGLISH
import org.apache.commons.validator.routines.InetAddressValidator
import org.apache.commons.validator.routines.UrlValidator
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

fun String.toMnemonicWords(expectedWordCount: Int): List<String> {
    val words = trim().split(" ").map { it.trim() }.filter { it.isNotEmpty() }
    if (words.size != expectedWordCount) return emptyList()
    return if (words.all { WORDLIST_ENGLISH.contains(it) }) {
        words
    } else {
        emptyList()
    }
}

fun String.formattedSpans(
    boldStyle: SpanStyle
): AnnotatedString {
    val singleAsteriskRegex = "(?<!\\*)\\*(?!\\*).*?(?<!\\*)\\*(?!\\*)".toRegex()
    val doubleAsteriskRegex = "(?<!\\*\\*)\\*\\*(?!\\*\\*).*?(?<!\\*\\*)\\*\\*(?!\\*\\*)".toRegex()
    val annotatedWords = doubleAsteriskRegex.findAll(input = this).map {
        it.value
    }.toList() + singleAsteriskRegex.findAll(input = this).map {
        it.value
    }.toList()
    return buildAnnotatedString {
        var startIndex = 0
        val inputText = this@formattedSpans
        annotatedWords.forEach { word ->
            val indexOfThisWord = inputText.indexOf(word)
            append(inputText.substring(startIndex, indexOfThisWord))

            startIndex = indexOfThisWord + word.length
            val strippedFromAnnotations = word.removeSurrounding("**").removeSurrounding("*")
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

// TODO this does not work for IPv6 thus we can't add a IPv6 address in gateway settings
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

// see the SanitizeAndValidateGatewayUrlTest for better understanding
fun String.sanitizeAndValidateGatewayUrl(isDevModeEnabled: Boolean = false): String? {
    val ipValidator = InetAddressValidator.getInstance()
    val urlValidator = UrlValidator.getInstance()

    return if (isDevModeEnabled) { // when dev mode is enabled we can accept IPs, IP:Port, http, and https
        if (urlValidator.isValid(this)) { // valid url meaning that contains http or https
            if (this.endsWith('/').not()) {
                "$this/"
            } else {
                this
            }
        } else {
            val urlToValidate = this.removeSuffix("/")
            if (ipValidator.isValidInet6Address(urlToValidate)) {
                "http://[$urlToValidate]/"
            } else if (urlToValidate.startsWith("https://")) {
                urlToValidate.substring(0, 8) + '[' + urlToValidate.substring(8) + "]/"
            } else if (urlToValidate.startsWith("http://")) {
                urlToValidate.substring(0, 7) + '[' + urlToValidate.substring(7) + "]/"
            } else {
                "http://$urlToValidate/"
            }
        }
    } else { // when dev mode is disabled we should not accept ONLY https - no IPs
        val urlWithoutHttp = this.removePrefix("http://").removePrefix("https://")
        if (urlWithoutHttp.contains(":")) { // if true it means it has a port or it is an IPv6
            null // then do not accept it
        } else {
            // now we need to check also for
            val urlToValidate = urlWithoutHttp
                .substringBefore("/")
                .removeSuffix("/")
            if (ipValidator.isValidInet4Address(urlToValidate) || ipValidator.isValidInet6Address(urlToValidate)) {
                null // then do not accept it
            } else {
                "https://$urlToValidate/"
            }
        }
    }
}
