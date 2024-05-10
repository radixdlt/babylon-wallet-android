package rdx.works.core.sargon

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun String.toUrl() = toHttpUrl()
fun String.toUrlOrNull() = toHttpUrlOrNull()
