package rdx.works.peerdroid.helpers

import java.security.MessageDigest

fun ByteArray.sha256(): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(this)
}

fun ByteArray.toHexString(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
