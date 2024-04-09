package rdx.works.core

import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.toBagOfBytes

@OptIn(ExperimentalUnsignedTypes::class)
fun List<UByte>.toByteArray() = toUByteArray().toByteArray()

fun ByteArray.hash(): Exactly32Bytes = toBagOfBytes().hash()

fun ByteArray.toHexString(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
