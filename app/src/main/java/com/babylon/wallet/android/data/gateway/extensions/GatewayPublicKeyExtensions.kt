package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyType
import com.radixdlt.sargon.extensions.hex

typealias GatewayPublicKey = com.babylon.wallet.android.data.gateway.generated.models.PublicKey
typealias GatewayPublicKeyEddsaEd25519 = com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEddsaEd25519
typealias GatewayPublicKeyEcdsaSecp256k1 = com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEcdsaSecp256k1

fun com.radixdlt.sargon.PublicKey.asGatewayPublicKey(): GatewayPublicKey = when (this) {
    is com.radixdlt.sargon.PublicKey.Ed25519 -> GatewayPublicKeyEddsaEd25519(
        keyType = PublicKeyType.EddsaEd25519,
        keyHex = hex
    )

    is com.radixdlt.sargon.PublicKey.Secp256k1 -> GatewayPublicKeyEcdsaSecp256k1(
        keyType = PublicKeyType.EcdsaSecp256k1,
        keyHex = hex
    )
}
