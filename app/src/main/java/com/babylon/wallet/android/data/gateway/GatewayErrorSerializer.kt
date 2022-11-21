package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.GatewayError
import com.babylon.wallet.android.data.gateway.generated.model.InternalServerError
import com.babylon.wallet.android.data.gateway.generated.model.InvalidRequestError
import com.babylon.wallet.android.data.gateway.generated.model.InvalidTransactionError
import com.babylon.wallet.android.data.gateway.generated.model.NotSyncedUpError
import com.babylon.wallet.android.data.gateway.generated.model.TransactionNotFoundError
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object GatewayErrorSerializer : JsonContentPolymorphicSerializer<GatewayError>(GatewayError::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out GatewayError> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "TransactionNotFoundError" -> TransactionNotFoundError.serializer()
            "InvalidRequestError" -> InvalidRequestError.serializer()
            "InvalidTransactionError" -> InvalidTransactionError.serializer()
            "NotSyncedUpError" -> NotSyncedUpError.serializer()
            else -> InternalServerError.serializer()
        }
    }
}
