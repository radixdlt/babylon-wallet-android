package com.babylon.wallet.android.data.transaction

import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignedIntentHash
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.bytes
import rdx.works.core.crypto.PrivateKey
import rdx.works.core.sargon.transactionSigningFactorInstance
import rdx.works.core.toByteArray

data class NotaryAndSigners(
    val signers: List<ProfileEntity>,
    private val ephemeralNotaryPrivateKey: PrivateKey
) {
    val notaryIsSignatory: Boolean
        get() = signers.isEmpty()

    fun notaryPublicKeyNew(): PublicKey {
        return ephemeralNotaryPrivateKey.publicKey()
    }

    fun signersPublicKeys(): List<PublicKey> = signers.map { signer ->
        signer.securityState.transactionSigningFactorInstance.publicKey.publicKey
    }

    fun signWithNotary(signedIntentHash: SignedIntentHash): Signature {
        return ephemeralNotaryPrivateKey.signToSignature(signedIntentHash.hash.bytes.bytes.toByteArray())
    }
}
