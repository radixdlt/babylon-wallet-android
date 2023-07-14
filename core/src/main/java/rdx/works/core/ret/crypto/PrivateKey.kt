@file:Suppress("MagicNumber")

package rdx.works.core.ret.crypto

import com.radixdlt.ret.PublicKey
import com.radixdlt.ret.Signature
import com.radixdlt.ret.SignatureWithPublicKey
import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.util.encoders.Hex
import rdx.works.core.toUByteList
import java.math.BigInteger
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.Security

/**
 * A class that provides key-pair and signature abstraction to all the supported curves used for
 * digital signatures.
 *
 * Note: this is a purely experimental implementation of the different cryptographic primitives
 * required to sign messages. This implementation has not been tested enough and should be checked
 * at some point in the future.
 */
sealed class PrivateKey {
    class EcdsaSecp256k1(
        private val privateKey: ECPrivateKeyParameters,
        private val publicKey: ECPublicKeyParameters
    ) : PrivateKey() {
        companion object {
            /** Defines the length of the private key [ByteArray] representation */
            private const val LENGTH: Int = 32

            /** A constant string that defines the name of the curve */
            private const val CURVE_NAME: String = "secp256k1"

            /** Defines the curve that this keypair is for, primarily for internal computations. */
            private val CURVE: X9ECParameters = CustomNamedCurves.getByName(CURVE_NAME)

            /** Defines the order of the curve. */
            private val CURVE_ORDER: BigInteger = CURVE.n

            /** Defines the half curve order. */
            private val HALF_CURVE_ORDER: BigInteger = CURVE_ORDER.shiftRight(1)

            /** Defines the domain parameters for this key-pair based on the [CURVE]. */
            private val DOMAIN: ECDomainParameters =
                ECDomainParameters(CURVE.curve, CURVE.g, CURVE.n, CURVE.h)

            /** Defines the spec of the key-pair based on the [CURVE]. */
            private val SPEC: ECParameterSpec =
                ECParameterSpec(CURVE.curve, CURVE.g, CURVE.n, CURVE.h)

            /**
             * Creates a new [EcdsaSecp256k1] object with a random private key.
             *
             * @return A new randomly generated [EcdsaSecp256k1] keypair.
             */
            fun newRandom(): EcdsaSecp256k1 {
                // Creating and initializing a new generator which will create the new random
                // private key for us.
                val generator = ECKeyPairGenerator()
                val keygenParams = ECKeyGenerationParameters(DOMAIN, SecureRandom())
                generator.init(keygenParams)

                // Generating the new random key-pair
                val keypair: AsymmetricCipherKeyPair = generator.generateKeyPair()
                val privateKeyParameters: ECPrivateKeyParameters =
                    keypair.private as ECPrivateKeyParameters
                val publicKeyParameters: ECPublicKeyParameters =
                    keypair.public as ECPublicKeyParameters

                return EcdsaSecp256k1(privateKeyParameters, publicKeyParameters)
            }

            /**
             * Creates a new [EcdsaSecp256k1] object from the passed private key bytes.
             *
             * This method is responsible for creating new key-pair params ([ECPrivateKeyParameters]
             * , [ECPublicKeyParameters]) given the bytes of the private key.
             *
             * @param privateKeyBytes The [ByteArray] representation of the private key.
             * @return The newly instantiated [EcdsaSecp256k1] private key from the private key
             *   bytes.
             */
            fun newFromPrivateKeyBytes(
                privateKeyBytes: ByteArray,
            ): EcdsaSecp256k1 {
                // Ensure that a valid private key length is provided
                assert(privateKeyBytes.size == LENGTH)

                // Getting the value of D based on the passed private-key bytearray.
                val d = BigInteger(1, privateKeyBytes)
                val privateKeyParameters = ECPrivateKeyParameters(d, DOMAIN)

                // Generating a public key spec from the D derived above
                val publicKeySpec = ECPublicKeySpec(SPEC.g.multiply(d), SPEC)

                // Generating the public key from the private key
                val publicKey: ECPublicKey =
                    KeyFactory.getInstance("EC", "BC").generatePublic(publicKeySpec) as ECPublicKey
                val publicKeyParameters = ECPublicKeyParameters(publicKey.q, DOMAIN)

                return EcdsaSecp256k1(privateKeyParameters, publicKeyParameters)
            }

            /**
             * Creates a new [EcdsaSecp256k1] object from the passed private key hex string.
             *
             * This method takes the hex-string representation of the private key and creates a new
             * [EcdsaSecp256k1] from it. It performs that by decoding the passed hex string and then
             * calls the [newFromPrivateKeyBytes] function for instantiation.
             *
             * @param privateKeyHexString The [String] representation of the private key.
             * @return The newly instantiated [EcdsaSecp256k1] private key from the private key hex
             *   string.
             */
            fun newFromPrivateKeyHexString(privateKeyHexString: String): EcdsaSecp256k1 {
                return newFromPrivateKeyBytes(Hex.decode(privateKeyHexString))
            }
        }

        // Signature methods
        override fun sign(hashedData: ByteArray): ByteArray {
            if (hashedData.size != NUMBER_OF_32_BYTES) {
                error("Length is invalid (${hashedData.size}). Did you forget to hash the input before calling sign?")
            }

            // Creating and initializing a new signer.
            val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
            signer.init(true, ECPrivateKeyParameters(BigInteger(1, toByteArray()), DOMAIN))

            val signatureComponents: Array<BigInteger> = signer.generateSignature(hashedData)
            val r: BigInteger = signatureComponents[0]
            var s: BigInteger = signatureComponents[1]

            // Enforcing low S (Seems to also be done by the library used in Scrypto)
            if (s > HALF_CURVE_ORDER) {
                s = CURVE_ORDER.subtract(s)
            }

            // With the available information, the recovery id (otherwise known as `v`) may be
            // calculated.
            val v: Int =
                ECKeyUtils.calculateV(
                    BigInteger(1, Bytes.bigIntegerToBytes(r, 32)),
                    BigInteger(1, Bytes.bigIntegerToBytes(s, 32)),
                    publicKeyByteArray(false),
                    hashedData
                )

            // All required information is now present to form the final 65-byte long byte array
            // of the signature
            return (
                byteArrayOf(v.toByte()) +
                    Bytes.bigIntegerToBytes(r, 32) +
                    Bytes.bigIntegerToBytes(s, 32)
                )
        }

        override fun signToSignature(hashedData: ByteArray): Signature.EcdsaSecp256k1 {
            return Signature.EcdsaSecp256k1(sign(hashedData).toUByteList())
        }

        override fun signToSignatureWithPublicKey(
            hashedData: ByteArray
        ): SignatureWithPublicKey.EcdsaSecp256k1 {
            return SignatureWithPublicKey.EcdsaSecp256k1(signToSignature(hashedData).value)
        }

        // Private Key repr methods

        override fun toByteArray(): ByteArray {
            return ECKeyUtils.adjustArray(privateKey.d.toByteArray(), LENGTH)
        }

        // Public Key methods

        override fun publicKey(): PublicKey.EcdsaSecp256k1 {
            return PublicKey.EcdsaSecp256k1(publicKeyByteArray(true).toUByteList())
        }

        private fun publicKeyByteArray(compressed: Boolean): ByteArray {
            return publicKey.q.getEncoded(compressed)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EcdsaSecp256k1

            return this.toByteArray().contentEquals(other.toByteArray())
        }

        override fun hashCode(): Int {
            var result = privateKey.hashCode()
            result = 31 * result + publicKey.hashCode()
            return result
        }

        constructor(
            privateKeyBytes: ByteArray
        ) : this(
            newFromPrivateKeyBytes(privateKeyBytes).privateKey,
            newFromPrivateKeyBytes(privateKeyBytes).publicKey,
        )

        constructor(privateKeyBytesHexString: String) : this(Hex.decode(privateKeyBytesHexString))
    }

    class EddsaEd25519(
        private val privateKey: Ed25519PrivateKeyParameters,
        private val publicKey: Ed25519PublicKeyParameters
    ) : PrivateKey() {
        companion object {
            /** Defines the length of the private key [ByteArray] representation */
            private const val LENGTH: Int = 32

            /**
             * Creates a new [EddsaEd25519] object with a random private key.
             *
             * @return A new randomly generated [EddsaEd25519] keypair.
             */
            fun newRandom(): EddsaEd25519 {
                // Creating and initializing a new generator which will create the new random
                // private key for us.
                val generator = Ed25519KeyPairGenerator()
                val keygenParams = Ed25519KeyGenerationParameters(SecureRandom())
                generator.init(keygenParams)

                // Generating the new random key-pair
                val keypair: AsymmetricCipherKeyPair = generator.generateKeyPair()
                val privateKeyParameters: Ed25519PrivateKeyParameters =
                    keypair.private as Ed25519PrivateKeyParameters
                val publicKeyParameters: Ed25519PublicKeyParameters =
                    keypair.public as Ed25519PublicKeyParameters

                return EddsaEd25519(privateKeyParameters, publicKeyParameters)
            }

            /**
             * Creates a new [EddsaEd25519] object from the passed private key bytes.
             *
             * This method is responsible for creating new key-pair params
             * ([Ed25519PrivateKeyParameters] , [Ed25519PublicKeyParameters]) given the bytes of the
             * private key.
             *
             * @param privateKeyBytes The [ByteArray] representation of the private key.
             * @return The newly instantiated [EddsaEd25519] private key from the private key bytes.
             */
            @Suppress("ForbiddenComment")
            fun newFromPrivateKeyBytes(privateKeyBytes: ByteArray): EddsaEd25519 {
                // Ensure that a valid private key length is provided
                assert(privateKeyBytes.size == LENGTH)

                val privateKeyParameters = Ed25519PrivateKeyParameters(privateKeyBytes)
                // TODO: Deal with the case of null when the private key is not valid
                val publicKeyParameters: Ed25519PublicKeyParameters =
                    privateKeyParameters.generatePublicKey()!!

                return EddsaEd25519(privateKeyParameters, publicKeyParameters)
            }

            /**
             * Creates a new [EddsaEd25519] object from the passed private key hex string.
             *
             * This method takes the hex-string representation of the private key and creates a new
             * [EddsaEd25519] from it. It performs that by decoding the passed hex string and then
             * calls the [newFromPrivateKeyBytes] function for instantiation.
             *
             * @param privateKeyHexString The [String] representation of the private key.
             * @return The newly instantiated [EddsaEd25519] private key from the private key hex
             *   string.
             */
            fun newFromPrivateKeyHexString(privateKeyHexString: String): EddsaEd25519 {
                return newFromPrivateKeyBytes(Hex.decode(privateKeyHexString))
            }
        }

        override fun sign(hashedData: ByteArray): ByteArray {
            if (hashedData.size != NUMBER_OF_32_BYTES) {
                error("Length is invalid (${hashedData.size}). Did you forget to hash the input before calling sign?")
            }

            // Creating and initializing the signer - This is the object responsible for the
            // creation of signatures on our behalf.
            val signer = Ed25519Signer()
            signer.init(true, privateKey)
            signer.update(hashedData, 0, hashedData.size)

            // Getting the signature of the message
            return signer.generateSignature()
        }

        override fun signToSignature(hashedData: ByteArray): Signature.EddsaEd25519 {
            return Signature.EddsaEd25519(sign(hashedData).toUByteList())
        }

        override fun signToSignatureWithPublicKey(
            hashedData: ByteArray
        ): SignatureWithPublicKey.EddsaEd25519 {
            return SignatureWithPublicKey.EddsaEd25519(signToSignature(hashedData).value, publicKey().value)
        }

        // Private Key repr methods

        override fun toByteArray(): ByteArray {
            return privateKey.encoded
        }

        // Public Key methods

        override fun publicKey(): PublicKey.EddsaEd25519 {
            return PublicKey.EddsaEd25519(publicKey.encoded.toUByteList())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EddsaEd25519

            return this.toByteArray().contentEquals(other.toByteArray())
        }

        override fun hashCode(): Int {
            var result = privateKey.hashCode()
            result = 31 * result + publicKey.hashCode()
            return result
        }

        constructor(
            privateKeyBytes: ByteArray
        ) : this(
            newFromPrivateKeyBytes(privateKeyBytes).privateKey,
            newFromPrivateKeyBytes(privateKeyBytes).publicKey,
        )

        constructor(privateKeyBytesHexString: String) : this(Hex.decode(privateKeyBytesHexString))
    }

    // Signature methods

    /**
     * Signs raw data and returns a [ByteArray] containing the raw signature
     *
     * An abstract method which needs to be implemented by classes that inherit [PrivateKey]. This
     * method is abstract as the signing logic differs between different curves.
     * Thus, this abstract function defines the interface and leaves the implementation to [PrivateKey]s.
     *
     * @param hashedData The hashed data to sign
     * @return A [ByteArray] containing the raw signature.
     */
    abstract fun sign(hashedData: ByteArray): ByteArray

    /**
     * Signs raw data and returns a [Signature] object of the signature
     *
     * @param hashedData The hashed data to sign
     * @return A [Signature] object of the signature.
     */
    abstract fun signToSignature(hashedData: ByteArray): Signature

    /**
     * Signs raw data and returns a [SignatureWithPublicKey] object of the signature with the public
     * key of the signer
     *
     * @param hashedData The hashed data to sign
     * @return A [SignatureWithPublicKey] object of the signature and the signer's public key.
     */
    abstract fun signToSignatureWithPublicKey(hashedData: ByteArray): SignatureWithPublicKey

    // Private Key repr methods

    /**
     * Represents the private key as a [ByteArray].
     *
     * @return A [ByteArray] representation of the private key
     */
    abstract fun toByteArray(): ByteArray

    /**
     * Represents the private key as a [String].
     *
     * @return A [String] representation of the private key
     */
    override fun toString(): String {
        return Hex.toHexString(toByteArray())
    }

    // Public Key methods

    /**
     * A getter method for the [PublicKey]
     *
     * @return The [PublicKey] associated with this [PrivateKey].
     */
    abstract fun publicKey(): PublicKey

    /**
     * The purpose of the following companion object is to have a static method that runs to ensure
     * that the required providers for the [PrivateKey] class to function correctly have been added.
     */
    companion object {
        private const val NUMBER_OF_32_BYTES = 32

        init {
            // Load the Bouncy Castle Provider
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.addProvider(BouncyCastleProvider())
        }
    }
}
