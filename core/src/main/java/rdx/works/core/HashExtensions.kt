package rdx.works.core

import com.radixdlt.ret.hash

@OptIn(ExperimentalUnsignedTypes::class)
fun List<UByte>.toByteArray() = toUByteArray().toByteArray()

fun ByteArray.blake2Hash(): ByteArray = hash(data = this).bytes()

fun String.blake2Hash(): ByteArray = toByteArray().blake2Hash()

fun ByteArray.toHexString(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
