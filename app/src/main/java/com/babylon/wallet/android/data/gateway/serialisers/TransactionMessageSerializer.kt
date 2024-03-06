package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.coreapi.EncryptedTransactionMessage
import com.babylon.wallet.android.data.gateway.coreapi.PlaintextTransactionMessage
import com.babylon.wallet.android.data.gateway.coreapi.TransactionMessage
import com.babylon.wallet.android.data.gateway.coreapi.TransactionMessageType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object TransactionMessageSerializer :
    JsonContentPolymorphicSerializer<TransactionMessage>(TransactionMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TransactionMessage> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            TransactionMessageType.plaintext.value -> PlaintextTransactionMessage.serializer()
            TransactionMessageType.encrypted.value -> EncryptedTransactionMessage.serializer()
            else -> error("No serializer found for TransactionMessage type.")
        }
    }
}
