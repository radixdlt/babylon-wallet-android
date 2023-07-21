package rdx.works.core.ret

import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.model.ECKeyPair
import com.radixdlt.model.PrivateKey
import com.radixdlt.ret.PublicKey
import com.radixdlt.ret.Signature
import com.radixdlt.ret.SignatureWithPublicKey
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.encoders.Hex
import rdx.works.core.blake2Hash
import rdx.works.core.ret.crypto.ECKeyUtils
import rdx.works.core.toByteArray
import rdx.works.core.toUByteList
import java.math.BigInteger

fun ECKeyPair.toEnginePublicKeyModel(): PublicKey {
    return when (this.publicKey.curveType) {
        EllipticCurveType.Secp256k1 -> {
            // Required size 33 bytes
            PublicKey.EcdsaSecp256k1(getCompressedPublicKey().toUByteList())
        }
        EllipticCurveType.Ed25519 -> {
            // Required size 32 bytes
            PublicKey.EddsaEd25519(getCompressedPublicKey().removeLeadingZero().toUByteList())
        }
        EllipticCurveType.P256 -> error("Curve EllipticCurveType.P256 not supported")
    }
}

fun PrivateKey.toEngineModel(): rdx.works.core.ret.crypto.PrivateKey {
    return when (this.curveType) {
        EllipticCurveType.Secp256k1 -> rdx.works.core.ret.crypto.PrivateKey.EcdsaSecp256k1.newFromPrivateKeyBytes(
            this.keyByteArray()
        )
        EllipticCurveType.Ed25519 -> rdx.works.core.ret.crypto.PrivateKey.EddsaEd25519.newFromPrivateKeyBytes(this.keyByteArray())
        EllipticCurveType.P256 -> error("Curve EllipticCurveType.P256 not supported")
    }
}

/**
 * A getter method for the [PublicKey].
 *
 * This method derives the public key associated with a given [Signature.EcdsaSecp256k1]
 * which is a process that requires access to the following information.
 * 1. The recovery id (otherwise known as `v`) of the public key - This is the first byte in
 *    the 65-byte long [Signature.EcdsaSecp256k1].
 * 2. The `r` parameter of the signature - These are the subsequent 32-bytes in the
 *    [Signature.EcdsaSecp256k1].
 * 3. The `s` parameter of the signature - These are the subsequent 32-bytes in the
 *    [Signature.EcdsaSecp256k1].
 * 4. The message that was signed - This is not stored anywhere in the signature and needs
 *    to be provided as an argument to this method.
 *
 * @param message The message which was used to create the signature. Note that this is the
 *   actual message and not a hash of the message.
 * @return The public key associated with the signature
 */
@Suppress("MagicNumber")
fun SignatureWithPublicKey.EcdsaSecp256k1.publicKey(message: ByteArray): PublicKey.EcdsaSecp256k1 {
    // Extracting the v, r, and s parameters from the 65-byte long signature.
    val signatureBytes: ByteArray = signature.toByteArray()

    val v: Byte = signatureBytes[0]
    val r = BigInteger(1, signatureBytes.sliceArray(1..32))
    val s = BigInteger(1, signatureBytes.sliceArray(33..64))

    // We expect that the raw message is passed (not the hash), therefore, we need to hash
    // the message to get to the expected output
    val hashedMessage: ByteArray = message.blake2Hash()

    // Derive the ECPoint of the public key from the above parameters. Note: we use `get` on
    // the return of the below function assuming that the signature is a valid signature
    // that can produce a valid ecPoint. It might be better to handle this with an exception
    // or not use `get` optimistically.
    val ecPoint: ECPoint =
        ECKeyUtils.recoverFromSignature(v.toInt(), r, s, hashedMessage).get()

    // Getting the bytes of the compressed public key (below, true = compress) and then
    // creating a new public key object.
    val publicKeyBytes: ByteArray = ecPoint.getEncoded(true)
    return PublicKey.EcdsaSecp256k1(publicKeyBytes.toUByteList())
}
fun SignatureWithPublicKey.EddsaEd25519.publicKey() = PublicKey.EddsaEd25519(publicKey)

fun SignatureWithPublicKey.EcdsaSecp256k1.signature() = Signature.EcdsaSecp256k1(signature)
fun SignatureWithPublicKey.EddsaEd25519.signature() = Signature.EddsaEd25519(signature)

fun Signature.EcdsaSecp256k1.toHexString(): String = Hex.toHexString(value.toByteArray())
fun Signature.EddsaEd25519.toHexString(): String = Hex.toHexString(value.toByteArray())

fun PublicKey.EcdsaSecp256k1.toHexString(): String = Hex.toHexString(value.toByteArray())
fun PublicKey.EddsaEd25519.toHexString(): String = Hex.toHexString(value.toByteArray())
