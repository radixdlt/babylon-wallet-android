package com.babylon.wallet.android.extensions

fun String.truncatedHash(): String {
    val first = substring(IntRange(0, 3))
    val last = substring(IntRange(length-9, length-1))
    return "$first...$last"
}