package com.babylon.wallet.android.data.transaction

import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.toECKeyPair
import com.radixdlt.toolkit.models.crypto.PrivateKey
import com.radixdlt.toolkit.models.crypto.PublicKey
import com.radixdlt.toolkit.models.crypto.Signature
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.utils.toEnginePublicKeyModel
import com.radixdlt.model.PrivateKey as SLIP10PrivateKey

data class NotaryAndSigners(
    val signers: List<Entity>,
    val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom()
) {
    val notaryAsSignatory: Boolean
        get() = signers.isEmpty()

    fun notaryPrivateKeySLIP10(): SLIP10PrivateKey {
        return SLIP10PrivateKey(ephemeralNotaryPrivateKey.toByteArray(), EllipticCurveType.Ed25519)
    }

    fun notaryPublicKey(): PublicKey {
        return notaryPrivateKeySLIP10()
            .toECKeyPair()
            .toEnginePublicKeyModel()
    }

    fun signWithNotary(data: ByteArray): Signature {
        return ephemeralNotaryPrivateKey.signToSignature(data)
    }
}
