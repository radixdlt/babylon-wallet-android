package com.babylon.wallet.android.utils

import com.radixdlt.sargon.extensions.bytes
import rdx.works.core.decodeHex
import rdx.works.core.hash
import rdx.works.core.toByteArray
import timber.log.Timber

fun parseEncryptionKeyFromConnectionPassword(connectionPassword: String): ByteArray? {
    return try {
        connectionPassword.decodeHex()
    } catch (iae: IllegalArgumentException) {
        Timber.e("failed to parse encryption key from connection id: ${iae.localizedMessage}")
        null
    }
}

fun getSignatureMessageFromConnectionPassword(connectionPassword: String): ByteArray {
    val prefix = "L".encodeToByteArray()
    val password = connectionPassword.decodeHex()
    val message = prefix + password
    return message.hash().bytes.toByteArray()
}
