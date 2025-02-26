package rdx.works.core.sargon

import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.hex
import kotlin.jvm.Throws

@Throws(CommonException::class)
fun SignatureWithPublicKey.Companion.init(
    signature: Signature,
    publicKey: PublicKey
): SignatureWithPublicKey = when (publicKey) {
    is PublicKey.Ed25519 -> {
        val signatureEd25519 = (signature as? Signature.Ed25519)?.value ?: throw CommonException.InvalidEd25519PublicKeyFromBytes(
            badValue = signature.bytes.hex
        )

        SignatureWithPublicKey.Ed25519(
            publicKey = publicKey.v1,
            signature = signatureEd25519
        )
    }

    is PublicKey.Secp256k1 -> {
        val signatureSecp256k1 = (signature as? Signature.Secp256k1)?.value ?: throw CommonException.InvalidSecp256k1PublicKeyFromBytes(
            badValue = signature.bytes.hex
        )

        SignatureWithPublicKey.Secp256k1(
            publicKey = publicKey.v1,
            signature = signatureSecp256k1
        )
    }
}
