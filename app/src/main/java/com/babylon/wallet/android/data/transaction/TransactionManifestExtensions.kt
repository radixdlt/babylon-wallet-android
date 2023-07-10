@file:Suppress("TooGenericExceptionThrown")

package com.babylon.wallet.android.data.transaction

import com.radixdlt.ret.TransactionManifest

fun TransactionManifest.toPrettyString(): String {
    val blobSeparator = "\n"
    val blobPreamble = "BLOBS\n"
    val blobLabel = "BLOB\n"

    val instructionsFormatted = instructions().asStr()

    val blobsByByteCount = blobs().mapIndexed { index, bytes ->
        "$blobLabel[$index]: #${bytes.size} bytes"
    }.joinToString(blobSeparator)

    val blobsString = if (blobsByByteCount.isNotEmpty()) {
        listOf(blobPreamble, blobsByByteCount).joinToString(separator = blobSeparator)
    } else {
        ""
    }

    return "$instructionsFormatted$blobsString"
}

fun TransactionManifest.toStringWithoutBlobs(): String = instructions().asStr()
