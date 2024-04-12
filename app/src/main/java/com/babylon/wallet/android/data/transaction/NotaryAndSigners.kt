package com.babylon.wallet.android.data.transaction

import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignedIntentHash
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.init
import rdx.works.core.toByteArray
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.core.crypto.PrivateKey

data class NotaryAndSigners(
    val signers: List<Entity>,
    private val ephemeralNotaryPrivateKey: PrivateKey
) {
    val notaryIsSignatory: Boolean
        get() = signers.isEmpty()

    fun notaryPublicKeyNew(): PublicKey {
        return ephemeralNotaryPrivateKey.publicKey()
    }

    fun signersPublicKeys(): List<PublicKey> = signers.map { signer ->
        when (val securityState = signer.securityState) {
            is SecurityState.Unsecured -> {
                val publicKey = when (val badge = securityState.unsecuredEntityControl.transactionSigning.badge) {
                    is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
                        badge.publicKey
                    }
                }
                when (publicKey.curve) {
                    Slip10Curve.CURVE_25519 -> PublicKey.Ed25519.init(hex = publicKey.compressedData)
                    Slip10Curve.SECP_256K1 -> PublicKey.Secp256k1.init(hex = publicKey.compressedData)
                }
            }
        }
    }

    fun signWithNotary(signedIntentHash: SignedIntentHash): Signature {
        return ephemeralNotaryPrivateKey.signToSignature(signedIntentHash.hash.bytes.toByteArray())
    }
}
