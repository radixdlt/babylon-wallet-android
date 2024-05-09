package com.babylon.wallet.android.data.transaction

import com.radixdlt.sargon.NotarySignature
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.SignedIntentHash
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.ProfileEntity
import rdx.works.core.sargon.transactionSigningFactorInstance

data class NotaryAndSigners(
    val signers: List<ProfileEntity>,
    private val ephemeralNotaryPrivateKey: Curve25519SecretKey
) {
    val notaryIsSignatory: Boolean
        get() = signers.isEmpty()

    fun notaryPublicKeyNew(): PublicKey.Ed25519 {
        return ephemeralNotaryPrivateKey.toPublicKey()
    }

    fun signersPublicKeys(): List<PublicKey> = signers.map { signer ->
        signer.securityState.transactionSigningFactorInstance.publicKey.publicKey
    }

    fun signWithNotary(signedIntentHash: SignedIntentHash): NotarySignature {
        return ephemeralNotaryPrivateKey.notarize(signedIntentHash)
    }
}
