package com.babylon.wallet.android.data.transaction

import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.toECKeyPair
import com.radixdlt.hex.extensions.hexToByteArray
import com.radixdlt.hex.model.HexString
import com.radixdlt.ret.PublicKey
import com.radixdlt.ret.Signature
import rdx.works.core.ret.crypto.PrivateKey
import rdx.works.core.ret.toEnginePublicKeyModel
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import com.radixdlt.model.PrivateKey as SLIP10PrivateKey

data class NotaryAndSigners(
    val signers: List<Entity>,
    val ephemeralNotaryPrivateKey: PrivateKey
) {
    val notaryIsSignatory: Boolean
        get() = signers.isEmpty()

    fun notaryPublicKey(): PublicKey {
        return SLIP10PrivateKey(ephemeralNotaryPrivateKey.toByteArray(), EllipticCurveType.Ed25519)
            .toECKeyPair()
            .toEnginePublicKeyModel()
    }

    fun signersPublicKeys() = signers.map { signer ->
        when (val securityState = signer.securityState) {
            is SecurityState.Unsecured -> {
                val publicKey = when (val badge = securityState.unsecuredEntityControl.transactionSigning.badge) {
                    is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
                        badge.publicKey
                    }
                }
                when (publicKey.curve) {
                    Slip10Curve.CURVE_25519 -> PublicKey.Ed25519(
                        value = HexString(publicKey.compressedData).hexToByteArray()
                    )
                    Slip10Curve.SECP_256K1 -> PublicKey.Secp256k1(
                        value = HexString(publicKey.compressedData).hexToByteArray()
                    )
                }
            }
        }
    }

    fun signWithNotary(hashedData: ByteArray): Signature {
        return ephemeralNotaryPrivateKey.signToSignature(hashedData)
    }
}
