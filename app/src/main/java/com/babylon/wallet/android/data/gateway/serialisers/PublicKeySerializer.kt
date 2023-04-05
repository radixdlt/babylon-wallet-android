package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.PublicKey
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEcdsaSecp256k1
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEddsaEd25519
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object PublicKeySerializer :
    JsonContentPolymorphicSerializer<PublicKey>(
        PublicKey::class
    ) {

    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<PublicKey> {
        return when (element.jsonObject["key_type"]?.jsonPrimitive?.content) {
            PublicKeyType.eddsaEd25519.value -> PublicKeyEddsaEd25519.serializer()
            PublicKeyType.ecdsaSecp256k1.value -> PublicKeyEcdsaSecp256k1.serializer()
            else -> error("")
        }
    }
}
