package rdx.works.core

import java.security.SecureRandom

object NonceGenerator {

    @Suppress("MagicNumber")
    operator fun invoke(): UInt {
        val nonceBytes = ByteArray(UInt.SIZE_BYTES)
        SecureRandom().nextBytes(nonceBytes)
        var nonce = 0u
        nonceBytes.forEachIndexed { index, byte ->
            nonce = nonce or (byte.toUInt() shl 8 * index)
        }
        return nonce
    }
}
