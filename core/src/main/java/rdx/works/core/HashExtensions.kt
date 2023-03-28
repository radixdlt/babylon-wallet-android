package rdx.works.core

import com.radixdlt.crypto.hash.sha256.extensions.sha256
import com.radixdlt.hex.extensions.toHexString

fun ByteArray.sha256Hash(): ByteArray = this.sha256()

fun ByteArray.blake2Hash(): ByteArray = com.radixdlt.toolkit.hash(data = this)

fun String.blake2Hash(): ByteArray = toByteArray().blake2Hash()

fun ByteArray.toHexString(): String = this.toHexString()
