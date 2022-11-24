package rdx.works.profile

import com.radixdlt.crypto.hash.sha256.extensions.sha256
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.hex.extensions.toHexString

/**
 * Return Strong and unique identifier for FactorSourceID / FactorInstanceID
 * SHA256(SHA256(publicKey.compressedForm))
 */
fun ByteArray.hashToFactorId(): String = removeLeadingZero().sha256().sha256().toHexString()
