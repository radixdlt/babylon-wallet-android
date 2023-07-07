package rdx.works.core

import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.ret.hash

@OptIn(ExperimentalUnsignedTypes::class)
fun ByteArray.toUByteList() = toUByteArray().toTypedArray().toList()

@OptIn(ExperimentalUnsignedTypes::class)
fun List<UByte>.toByteArray() = toUByteArray().toByteArray()

fun ByteArray.blake2Hash(): ByteArray = hash(data = toUByteList()).bytes().toByteArray()

fun String.blake2Hash(): ByteArray = toByteArray().blake2Hash()

fun ByteArray.toHexString(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun String.compressedPublicKeyHash(): String = decodeHex().blake2Hash().takeLast(PUBLIC_KEY_HASH_LENGTH).toHexString()

fun String.compressedPublicKeyHashBytes(): ByteArray = decodeHex().blake2Hash().takeLast(PUBLIC_KEY_HASH_LENGTH).toByteArray()

const val PUBLIC_KEY_HASH_LENGTH = 29
