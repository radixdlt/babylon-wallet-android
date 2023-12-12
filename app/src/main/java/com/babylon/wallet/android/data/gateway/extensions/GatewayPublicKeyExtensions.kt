package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyType
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.ret.PublicKey

typealias GatewayPublicKey = com.babylon.wallet.android.data.gateway.generated.models.PublicKey
typealias GatewayPublicKeyEddsaEd25519 = com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEddsaEd25519
typealias GatewayPublicKeyEcdsaSecp256k1 = com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEcdsaSecp256k1

fun PublicKey.asGatewayPublicKey(): GatewayPublicKey = when (this) {
    is PublicKey.Ed25519 -> GatewayPublicKeyEddsaEd25519(
        keyType = PublicKeyType.eddsaEd25519,
        keyHex = value.toHexString()
    )

    is PublicKey.Secp256k1 -> GatewayPublicKeyEcdsaSecp256k1(
        keyType = PublicKeyType.ecdsaSecp256k1,
        keyHex = value.toHexString()
    )
}
