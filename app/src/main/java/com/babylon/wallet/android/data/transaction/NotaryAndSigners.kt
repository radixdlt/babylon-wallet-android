package com.babylon.wallet.android.data.transaction

import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.toECKeyPair
import com.radixdlt.ret.PublicKey
import com.radixdlt.ret.Signature
import rdx.works.core.ret.crypto.PrivateKey
import rdx.works.core.ret.toEnginePublicKeyModel
import rdx.works.profile.data.model.pernetwork.Entity
import com.radixdlt.model.PrivateKey as SLIP10PrivateKey

data class NotaryAndSigners(
    val signers: List<Entity>,
    val ephemeralNotaryPrivateKey: PrivateKey
) {
    val notaryIsSignatory: Boolean
        get() = signers.isEmpty()

    fun notaryPrivateKeySLIP10(): SLIP10PrivateKey {
        return SLIP10PrivateKey(ephemeralNotaryPrivateKey.toByteArray(), EllipticCurveType.Ed25519)
    }

    fun notaryPublicKey(): PublicKey {
        return notaryPrivateKeySLIP10()
            .toECKeyPair()
            .toEnginePublicKeyModel()
    }

    fun signWithNotary(hashedData: ByteArray): Signature {
        return ephemeralNotaryPrivateKey.signToSignature(hashedData)
    }
}
