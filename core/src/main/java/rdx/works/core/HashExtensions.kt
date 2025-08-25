package rdx.works.core

import com.radixdlt.sargon.Hash
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.toBagOfBytes

@OptIn(ExperimentalUnsignedTypes::class)
fun List<UByte>.toByteArray() = toUByteArray().toByteArray()

fun ByteArray.hash(): Hash = toBagOfBytes().hash()

// Unsafe helper for converting BagOfBytes to ByteArray when needed for platform APIs like IsoDep
@Suppress("NOTHING_TO_INLINE")
inline fun com.radixdlt.sargon.BagOfBytes.toByteArrayUnsafe(): ByteArray = this.toByteArray()

fun ByteArray.toHexString(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
