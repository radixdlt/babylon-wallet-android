package rdx.works.profile.ret.crypto

import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.encoders.Hex
import rdx.works.core.blake2Hash
import java.math.BigInteger

sealed interface SignatureWithPublicKey {

    val signature: ByteArray
    val signatureHex: String
        get() = Hex.toHexString(signature)

    class Ed25519(
        val publicKey: ByteArray,
        override val signature: ByteArray
    ): SignatureWithPublicKey {

        val publicKeyHex: String
            get() = Hex.toHexString(publicKey)

    }

    class Secp256k1(
        override val signature: ByteArray
    ): SignatureWithPublicKey {

        /**
         * A getter method for the PublicKey [ByteArray].
         *
         * This method derives the public key associated with a given Secp256k1 signature
         * which is a process that requires access to the following information.
         * 1. The recovery id (otherwise known as `v`) of the public key - This is the first byte in
         *    the 65-byte long [Signature.Secp256k1].
         * 2. The `r` parameter of the signature - These are the subsequent 32-bytes in the
         *    Secp256k1.
         * 3. The `s` parameter of the signature - These are the subsequent 32-bytes in the
         *    Secp256k1.
         * 4. The message that was signed - This is not stored anywhere in the signature and needs
         *    to be provided as an argument to this method.
         *
         * @param message The message which was used to create the signature. Note that this is the
         *   actual message and not a hash of the message.
         * @return The public key associated with the signature
         */
        fun publicKey(message: ByteArray): ByteArray {
            // Extracting the v, r, and s parameters from the 65-byte long signature.
            val signatureBytes: ByteArray = signature

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
                rdx.works.profile.ret.crypto.ECKeyUtils.recoverFromSignature(v.toInt(), r, s, hashedMessage).get()

            // Getting the bytes of the compressed public key (below, true = compress) and then
            // creating a new public key object.
            return ecPoint.getEncoded(true)
        }

        fun publicKeyHex(message: ByteArray): String = Hex.toHexString(publicKey(message))
    }

}