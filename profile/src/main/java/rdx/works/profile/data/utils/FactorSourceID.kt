package rdx.works.profile.data.utils

import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.hex.extensions.toHexString
import rdx.works.core.blake2Hash

/**
 * Return Strong and unique identifier for FactorSourceID / FactorInstanceID
 * blake2b(publicKey.compressedForm))
 *
 * We get compressed public key which has 33 bytes instead of 32 because slip-10 formats it like this
 * (by adding zero byte at front, to be the same size as for Secp256k1 elliptic curve)
 * Then we use blake2 hash and get hex version.
*/
fun ByteArray.hashToFactorId(): String = removeLeadingZero().blake2Hash().toHexString()
