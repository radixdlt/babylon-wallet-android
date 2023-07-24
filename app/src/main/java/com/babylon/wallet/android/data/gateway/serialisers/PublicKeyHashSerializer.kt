package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyHash
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyHashEcdsaSecp256k1
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyHashEddsaEd25519
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyHashType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object PublicKeyHashSerializer : JsonContentPolymorphicSerializer<PublicKeyHash>(PublicKeyHash::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PublicKeyHash> {
        return when (PublicKeyHashType.from(element.jsonObject["key_hash_type"]?.jsonPrimitive?.content.orEmpty())) {
            PublicKeyHashType.ecdsaSecp256k1 -> PublicKeyHashEcdsaSecp256k1.serializer()
            PublicKeyHashType.eddsaEd25519 -> PublicKeyHashEddsaEd25519.serializer()
        }
    }
}
