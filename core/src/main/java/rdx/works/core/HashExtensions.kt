package rdx.works.core

import com.radixdlt.hex.extensions.toHexString

fun ByteArray.blake2Hash(): ByteArray = com.radixdlt.toolkit.hash(data = this)

fun String.blake2Hash(): ByteArray = toByteArray().blake2Hash()

fun ByteArray.toHexString(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

@Suppress("MagicNumber")
fun String.compressedPublicKeyHash(): String = decodeHex().blake2Hash().takeLast(PUBLIC_KEY_HASH_LENGTH).toHexString()

@Suppress("MagicNumber")
fun String.compressedPublicKeyHashBytes(): ByteArray = decodeHex().blake2Hash().takeLast(PUBLIC_KEY_HASH_LENGTH).toByteArray()

const val PUBLIC_KEY_HASH_LENGTH = 29
