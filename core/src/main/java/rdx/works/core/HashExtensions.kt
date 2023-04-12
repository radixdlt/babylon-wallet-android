package rdx.works.core

fun ByteArray.blake2Hash(): ByteArray = com.radixdlt.toolkit.hash(data = this)

fun String.blake2Hash(): ByteArray = toByteArray().blake2Hash()

fun ByteArray.toHexString(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
