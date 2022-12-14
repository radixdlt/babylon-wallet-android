package com.babylon.wallet.android.utils

import okio.ByteString.Companion.decodeHex
import timber.log.Timber

fun parseEncryptionKeyFromConnectionPassword(connectionPassword: String): ByteArray? {
    return try {
        connectionPassword.decodeHex().toByteArray()
    } catch (iae: IllegalArgumentException) {
        Timber.e("failed to parse encryption key from connection id: ${iae.localizedMessage}")
        null
    }
}