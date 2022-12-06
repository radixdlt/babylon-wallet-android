package rdx.works.profile.data.utils

import com.radixdlt.crypto.hash.sha256.extensions.sha256
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.hex.extensions.toHexString

/**
 * Return Strong and unique identifier for FactorSourceID / FactorInstanceID
 * SHA256(SHA256(publicKey.compressedForm))
 *
 * We get compressed public key which has 33 bytes instead of 32 because slip-10 formats it like this
 * (by adding zero byte at front, to be the same size as for Secp256k1 elliptic curve)
 * Then we hash it with sha256 twice and get hex version.
*/
fun ByteArray.hashToFactorId(): String = removeLeadingZero().sha256().sha256().toHexString()
